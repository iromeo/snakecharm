package features.glue

import com.intellij.codeInsight.documentation.DocumentationManager
import com.intellij.codeInsight.highlighting.BraceMatchingUtil
import com.intellij.ide.util.gotoByName.GotoSymbolModel2
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.IncorrectOperationException
import com.intellij.util.containers.ContainerUtil
import com.jetbrains.snakecharm.FakeSnakemakeInjector
import features.glue.SnakemakeWorld.findPsiElementUnderCaret
import features.glue.SnakemakeWorld.fixture
import features.glue.SnakemakeWorld.myGeneratedDocPopupText
import io.cucumber.datatable.DataTable
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import java.io.File.separator
import java.util.regex.Pattern
import kotlin.test.*


class ActionsSteps {
    @When("^I expect inspection (error|warning|info|TYPO|weak warning) on <([^>]+)> with message$")
    fun iExpectInspectionOn(level: String, signature: String, message: String) {
        iExpectInspectionOnIn(level, signature, signature, message)
    }

    @Given("^I expect no inspection (error|warning|info|TYPO|weak warning)$")
    fun iExpectNoInspection(_level: String) {
        //Fake step, do nothing "check highlighting" step will show errors in such case
        //This step just for more readable tests
    }

    @Given("^I expect inspection (error|warning|info|TYPO|weak warning) with message \"(.*)\" on")
    fun iExpectInspectionWithMessageOn(level: String, message: String, signature: String) {
        val fixedSignature = signature.replace("\r", "")
        iExpectInspectionOnIn(level, fixedSignature, fixedSignature, message)
    }

    @Given("^I expect reference highlighting on \"(.+)\"")
    fun iExpectReferenceHighlighting(signature: String) {
        ApplicationManager.getApplication().invokeAndWait {
            val fixture = fixture()
            val psiFile = fixture.file
            val document = PsiDocumentManager.getInstance(fixture.project).getDocument(psiFile)!!
            val pos = document.text.indexOf(signature)
            assertTrue(
                    pos >= 0,
                    "Signature <$signature> wasn't found in the file ${psiFile.name}."

            )
            val reference = fixture.getReferenceAtCaretPosition()
            assertNotNull(reference, message = "There is no reference at the caret position")
            assertEquals(
                    signature,
                    reference.canonicalText,
                    message = "Expected highlighted text wasn't equal to the actual one."
            )
        }
    }


    @Given("^I expect inspection (error|warning|info|TYPO|weak warning) on <([^>]+)> in <(.+)> with message$")
    fun iExpectInspectionOnIn(level: String, text: String, signature: String, message: String) {
        wrapTextInHighlightingTags(level, text, signature, message)
    }

    @When("^I expect inspection (error|warning|info|TYPO|weak warning) on <([^>]+)> with messages$")
    fun iExpectInspectionsOnWithMessages(level: String, signature: String, messages: List<String>) {
        iExpectInspectionsOnInWithMessages(level, signature, signature, messages)
    }

    @Given("^I expect inspection (error|warning|info|TYPO|weak warning) on <([^>]+)> in <(.+)> with messages$")
    fun iExpectInspectionsOnInWithMessages(level: String, text: String, signature: String, messages: List<String>) {
        messages.forEach { message ->
            wrapTextInHighlightingTags(level, text, signature, message, true)
        }
    }

    private fun wrapTextInHighlightingTags(
            highlightingLevel: String,
            text: String,
            signature: String,
            message: String,
            searchInTags: Boolean = false
    ) {
        require(!Pattern.compile("(^|[^\\\\])\"").matcher(message).find()) {
            "Quotes (\") should be escaped (with \\) in message: $message"
        }

        val tag = highlightingLevel.replace(' ', '_')
        val newText = "<$tag descr=\"$message\">$text</$tag>"
        val fixture = fixture()
        val psiFile = fixture.file
        val project = psiFile.project
        val document = PsiDocumentManager.getInstance(fixture.project).getDocument(fixture.file)!!

        val posInsideTags = if (searchInTags) findTextPositionInsideTags(tag, text, document.text) else -1
        val startPos = if (posInsideTags == -1) {
            findTextPositionWithSignature(text, signature, document, psiFile)
        } else {
            posInsideTags
        }

        ApplicationManager.getApplication().invokeAndWait {
            performAction(project, Runnable {
                fixture.editor.document.replaceString(startPos, startPos + text.length, newText)
            })
        }
    }

    private fun findTextPositionInsideTags(
            tag: String,
            text: String,
            fullText: String
    ): Int {
        val textInsideTagsPattern = Pattern.compile("<$tag descr=.+?>([^<>]+)</$tag>")
        val matcher = textInsideTagsPattern.matcher(fullText)
        return if (matcher.find() && matcher.group(1) == text) matcher.start(1) else -1
    }

    private fun findTextPositionWithSignature(
            text: String,
            signature: String,
            document: Document,
            psiFile: PsiFile
    ): Int {
        val pos = document.text.indexOf(signature)
        assertTrue(
                pos >= 0,
                "Signature <$signature> wasn't found in the file ${psiFile.name}."
        )

        val posInSignature = signature.indexOf(text)
        assertTrue(posInSignature >= 0)
        return pos + posInSignature
    }

    @When("^I check highlighting (errors|warnings|infos|weak warnings)$")
    fun iCheckHighlighting(type: String) {
        checkHighlighting(type, false)
    }

    @When("^I check highlighting (errors|warnings|infos|weak warnings) ignoring extra highlighting$")
    fun iCheckHighlightingIgnoreExtra(type: String) {
        checkHighlighting(type, true)
    }

    fun checkHighlighting(type: String, ignoreExtra: Boolean) {
        val fixture = fixture()
        when (type) {
            "errors" -> fixture.checkHighlighting(false, false, false, ignoreExtra)
            "warnings" -> fixture.checkHighlighting(true, false, false, ignoreExtra)
            "infos" -> fixture.checkHighlighting(false, true, false, ignoreExtra)
            "weak warnings" -> fixture.checkHighlighting(false, false, true, ignoreExtra)
            else -> fail("Unknown highlighting type: $type")
        }
    }

    @When("^I invoke quick documentation popup$")
    fun iInvokeQuickDocumentationPopup() {
        generateDocumentation(false)
    }

    @When("^I invoke quick navigation info$")
    fun iInvokeQuickNavigationInfo() {
        // On: Ctrl + hover
        generateDocumentation(true)
    }

    @Then("^Documentation text should be equal to (.*)")
    fun documentationTextShouldBeEqualTo(str: String) {
        documentationTextShouldBeEqual(str)
    }

    @Then("^Documentation text should be equal to$")
    fun documentationTextShouldBeEqual(str: String) {
        val text = StringUtil.convertLineSeparators(str)
        val docPopupText = myGeneratedDocPopupText
        assertNotNull(docPopupText)
        assertEquals(text, docPopupText, "Expected <$text> to be equal to  <$docPopupText>")
    }

    @Then("^Documentation text should contain (.*)$")
    fun documentationShouldContain(text: String) {
        documentationTextShouldContain(text)
    }

    @Then("^Documentation text should contain$")
    fun documentationTextShouldContain(subStr: String) {
        var text = StringUtil.convertLineSeparators(subStr)
        val ptn = Pattern.compile(".*^(in.*smk)", Pattern.MULTILINE)
        val location = ContainerUtil.getFirstItem(StringUtil.findMatches(text, ptn))

        if (location != null) {
            val systemDependentLocation = location.replace("/", separator)
            text = text.replace(location, systemDependentLocation)
        }
        val docPopupText = myGeneratedDocPopupText
        assertNotNull(docPopupText)
        assertTrue(
                text in docPopupText,
                "Expected <$text> to be in <$docPopupText>"
        )
    }

    @When("^I invoke rename with name \"([^\"]+)\"$")
    fun iInvokeRenameWithName(newName: String) {
        ApplicationManager.getApplication().invokeAndWait {
                fixture().renameElementAtCaret(newName)
        }
    }

    @When("^I invoke rename with name \"(.+)\" and get error \"(.+)\"$")
    fun iInvokeRenameWithNameAndGetError(newName: String, expectedError: String) {
        ApplicationManager.getApplication().invokeAndWait {
            var actualError: String? = null
            try {
                fixture().renameElementAtCaret(newName)
            } catch (e: RuntimeException) {
                if (e.cause is IncorrectOperationException) {
                    actualError = e.cause!!.message
                } else {
                    throw e
                }
            }
            assertNotNull(actualError, "Error not occurred")
            assertEquals(expectedError, actualError)
        }
    }

    @Then("^I invoke quick fix ([^\\]]+) and see text:")
    fun iInvokeQuickFixAndSeeText(quickFixName: String, text: String) {
        val quickFix = fixture().getAllQuickFixes().first { it.familyName == quickFixName }
        ApplicationManager.getApplication().invokeAndWait {
            fixture().launchAction(quickFix)
        }
        fixture().checkResult(text.replace("\r", ""))
    }

    @Then("^go to symbol should contain:$")
    fun completionListShouldContain(table: DataTable) {
        val names = ApplicationManager.getApplication().runReadAction(Computable {
            val model = GotoSymbolModel2(fixture().project)
            model.getNames(false).toList()
        })
        val expected = table.asList()
        assertHasElements(names, expected)
    }


    @Then("^I expect backward brace matching before \"(.+)\"")
    fun iExpectBraceMatchingBefore(str: String) {
        iExpectBraceMatchingAt(str, false)
    }

    @Then("^I expect forward brace matching after \"(.+)\"")
    fun iExpectBraceMatchingAfter(str: String) {
        iExpectBraceMatchingAt(str, true)
    }

    private fun iExpectBraceMatchingAt(str: String, forward: Boolean) {
        ApplicationManager.getApplication().invokeAndWait {
            val fixture = fixture()
            var offset = fixture.file.text.indexOf(str)

            if (forward) {
                offset += str.length - 1
            }

            val matchOffset = BraceMatchingUtil.getMatchedBraceOffset(fixture.editor, forward, fixture.file)
            assertEquals(offset, matchOffset)
        }
    }

    @Then("^I expect language injection on \"(.*)\"")
    fun iExpectLanguageInjectionOn(str: String) {
        ApplicationManager.getApplication().invokeAndWait {
            val fixture = SnakemakeWorld.injectionFixture()
            val injectedFile = fixture.injectedElement?.containingFile
            assertNotNull(injectedFile, "No language was injected at caret position")
            assertEquals(str, injectedFile.text)
        }
    }

    @Then("^I inject SmkSL at a caret")
       fun iInjectSmkSLAtCaret() {
           ApplicationManager.getApplication().invokeAndWait {
               val fixture = fixture()
               val offsetUnderCaret = SnakemakeWorld.getOffsetUnderCaret()
               val elementAtOffset = fixture.file.findElementAt(offsetUnderCaret)
               requireNotNull(elementAtOffset) { "No element at a caret offset: $offsetUnderCaret" }

               // auto unregister when fixture is disposed
               InjectedLanguageManager.getInstance(fixture.project).registerMultiHostInjector(
                       FakeSnakemakeInjector(offsetUnderCaret), fixture.projectDisposable
               )
           }
       }

    @Then("^I expect no language injection")
    fun iExpectNoLanguageInjection() {
        ApplicationManager.getApplication().invokeAndWait {
            val fixture = SnakemakeWorld.injectionFixture()
            val element = fixture.injectedElement
            assertNull(element, "${element?.language} language was injected at caret position")
        }
    }

    private fun findTargetElementFor(element: PsiElement, editor: Editor) =
            DocumentationManager.getInstance(element.project)
                    .findTargetElement(editor, element.containingFile, element)

    private fun generateDocumentation(generateQuickDoc: Boolean) {
        ApplicationManager.getApplication().invokeAndWait {
            val element = findPsiElementUnderCaret()
            requireNotNull(element)

            val editor = fixture().editor
            val targetElement = findTargetElementFor(element, editor)
            val documentationProvider = DocumentationManager.getProviderFromElement(targetElement)

            myGeneratedDocPopupText = when {
                generateQuickDoc -> documentationProvider.getQuickNavigateInfo(
                        targetElement, element
                )
                else -> documentationProvider.generateDoc(
                        targetElement, element
                )
            }
        }
    }

    companion object {
        fun performAction(project: Project, action: Runnable) {
            ApplicationManager.getApplication().runWriteAction {
                CommandProcessor.getInstance().executeCommand(
                        project, action, "SnakeCharmTestCmd", null
                )
            }
        }
    }
}