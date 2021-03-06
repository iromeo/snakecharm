package com.jetbrains.snakecharm.stringLanguage.lang.psi.elementTypes

import com.intellij.lang.*
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.ILazyParseableElementType
import com.jetbrains.snakecharm.stringLanguage.SmkSLanguage
import org.intellij.lang.regexp.RegExpCapability
import org.intellij.lang.regexp.RegExpLanguage
import org.intellij.lang.regexp.RegExpParserDefinition
import java.util.*

class SmkSLRegExpElementType(debugName: String) : ILazyParseableElementType(debugName, SmkSLanguage) {
    companion object {
        // Our choice of capabilities is based on
        // pattern for matching regular expressions in Snakemake:
        // ([^{}]+ | \{\d+(,\d+)?\})*
        // See snakemake/io.py:486
        val CAPABILITIES: EnumSet<RegExpCapability> = EnumSet.of(
                RegExpCapability.OCTAL_NO_LEADING_ZERO,
                RegExpCapability.MIN_OCTAL_3_DIGITS
        )
    }
    override fun parseLight(chameleon: ASTNode): PsiBuilder {
        val project = chameleon.psi.project
        val regExpLanguage = RegExpLanguage.INSTANCE
        val definition =
                LanguageParserDefinitions.INSTANCE.forLanguage(regExpLanguage) as RegExpParserDefinition
        val lexer = definition.createLexer(project, CAPABILITIES)
        val parser = definition.createParser(project, CAPABILITIES)
        val builder = PsiBuilderFactory.getInstance().createBuilder(project, chameleon, lexer, regExpLanguage, chameleon.chars)
        (parser as LightPsiParser).parseLight(this, builder)

        return builder
    }

    override fun doParseContents(chameleon: ASTNode, psi: PsiElement): ASTNode? =
        parseLight(chameleon).treeBuilt.firstChildNode
}