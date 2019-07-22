package com.jetbrains.snakecharm.lang.psi.types

import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import com.jetbrains.python.psi.AccessDirection
import com.jetbrains.python.psi.PyExpression
import com.jetbrains.python.psi.PyReferenceExpression
import com.jetbrains.python.psi.resolve.PyResolveContext
import com.jetbrains.python.psi.resolve.RatedResolveResult
import com.jetbrains.python.psi.types.PyType
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpointArgsSection

class SmkSectionArgumentType(
        private val referenceExpression: PyReferenceExpression,
        private val containingSections: List<SmkRuleOrCheckpointArgsSection>
) : PyType {
    override fun getName() = referenceExpression.name

    override fun assertValid(p0: String?) {  }

    override fun isBuiltin() = false

    override fun resolveMember(
            name: String,
            location: PyExpression?,
            direction: AccessDirection,
            resolveContext: PyResolveContext
    ): List<RatedResolveResult> {
        val variants = mutableListOf<PsiElement>()
        containingSections.forEach { section ->
            val argument = section.keywordArguments?.firstOrNull { it.name == name }
            if (argument != null) {
                variants.add(argument)
            }
        }
        return variants.map { RatedResolveResult(RatedResolveResult.RATE_NORMAL, it) }
    }

    override fun getCompletionVariants(
            completionPrefix: String?,
            location: PsiElement,
            context: ProcessingContext?
    ): Array<Any> {
        val variants = mutableListOf<String>()
        containingSections.forEach { section ->
            section.keywordArguments?.forEach {
                val name = it.name
                if (name != null) {
                    variants.add(name)
                }
            }
        }
        return variants.map { LookupElementBuilder.create(it) }.toTypedArray()
    }
}