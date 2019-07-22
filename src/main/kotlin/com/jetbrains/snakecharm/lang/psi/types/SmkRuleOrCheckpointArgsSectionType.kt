package com.jetbrains.snakecharm.lang.psi.types

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.lookup.LookupItem
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import com.jetbrains.python.psi.AccessDirection
import com.jetbrains.python.psi.PyExpression
import com.jetbrains.python.psi.PyReferenceExpression
import com.jetbrains.python.psi.resolve.PyResolveContext
import com.jetbrains.python.psi.resolve.RatedResolveResult
import com.jetbrains.python.psi.types.PyType
import com.jetbrains.snakecharm.lang.SnakemakeNames
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpoint
import com.jetbrains.snakecharm.lang.psi.SmkSection

class SmkRuleOrCheckpointArgsSectionType(
        private val referenceExpression: PyReferenceExpression,
        private val containingRulesOrCheckpoints: List<SmkRuleOrCheckpoint>
) : PyType {
    override fun getName(): String? = referenceExpression.name

    override fun assertValid(p0: String?) {}

    override fun resolveMember(
            name: String,
            location: PyExpression?,
            direction: AccessDirection,
            resolveContext: PyResolveContext
    ): List<RatedResolveResult> {
        val variants = mutableListOf<SmkSection>()
        containingRulesOrCheckpoints.forEach { ruleLike ->
            // only resolve output section
            val section = ruleLike.getSections().firstOrNull { it.name == SnakemakeNames.SECTION_OUTPUT }
            if (section != null) {
                variants.add(section)
            }
        }
        return listOf(RatedResolveResult(RatedResolveResult.RATE_NORMAL, variants.first()))
        //return variants.map { RatedResolveResult(RatedResolveResult.RATE_NORMAL, it) }
    }

    override fun getCompletionVariants(
            completionPrefix: String?,
            location: PsiElement,
            context: ProcessingContext?
    ): Array<Any> {
        val variants = mutableListOf<LookupElement>()
        containingRulesOrCheckpoints.forEach {
            variants.add(
                    LookupElementBuilder.create(SnakemakeNames.SECTION_OUTPUT)
                            .withTypeText(it.containingFile.name)
            )
        }
        return variants.toTypedArray()
    }


    override fun isBuiltin() = false
}
