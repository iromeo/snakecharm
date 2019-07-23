package com.jetbrains.snakecharm.codeInsight

import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.psi.PyReferenceExpression
import com.jetbrains.python.psi.types.PyType
import com.jetbrains.python.psi.types.PyTypeProviderBase
import com.jetbrains.python.psi.types.TypeEvalContext
import com.jetbrains.snakecharm.lang.SnakemakeLanguageDialect
import com.jetbrains.snakecharm.lang.SnakemakeNames
import com.jetbrains.snakecharm.lang.SnakemakeNames.SMK_VARS_CHECKPOINTS
import com.jetbrains.snakecharm.lang.SnakemakeNames.SMK_VARS_RULES
import com.jetbrains.snakecharm.lang.psi.*
import com.jetbrains.snakecharm.lang.psi.types.SmkCheckPointsType
import com.jetbrains.snakecharm.lang.psi.types.SmkRuleOrCheckpointArgsSectionType
import com.jetbrains.snakecharm.lang.psi.types.SmkRulesType
import com.jetbrains.snakecharm.lang.psi.types.SmkSectionArgumentType

class SmkSectionTypeProvider : PyTypeProviderBase() {

    override fun getReferenceExpressionType(
            referenceExpression: PyReferenceExpression,
            context: TypeEvalContext
    ): PyType? {

        if (!SnakemakeLanguageDialect.isInsideSmkFile(referenceExpression)) {
            return null
        }

        val psiFile = referenceExpression.containingFile

        if (referenceExpression.children.isNotEmpty()) {
            val resolvedReference = referenceExpression.reference.multiResolve(false)

            val containingSections = resolvedReference.filter {
                it.element is SmkRuleOrCheckpointArgsSection &&
                        (it.element as SmkRuleOrCheckpointArgsSection).name in SmkRuleOrCheckpointArgsSection.RULE_PROXY_KEYWORDS
            }
            if (containingSections.isNotEmpty()) {
                return SmkSectionArgumentType(
                        referenceExpression,
                        containingSections.map { it.element as SmkRuleOrCheckpointArgsSection }
                )
            }

            val containingRulesOrCheckpoints = resolvedReference.filter { it.element is SmkRuleOrCheckpoint }
            if (containingRulesOrCheckpoints.isNotEmpty()) {
                val parentReference = referenceExpression.parent as? PyReferenceExpression
                if (parentReference != null) {
                    return SmkRuleOrCheckpointArgsSectionType(
                            referenceExpression.parent as PyReferenceExpression,
                            containingRulesOrCheckpoints.map { it.element as SmkRuleOrCheckpoint }
                    )
                }
            }

            // part of some other longer reference
            return null
        }
        // XXX: at the moment affects all "rules" variables in a *.smk file, better to
        // affect only "rules" which is resolved to appropriate place
        return when (referenceExpression.referencedName) {
            SMK_VARS_RULES -> SmkRulesType(
                    PsiTreeUtil.getParentOfType(referenceExpression, SmkRule::class.java),
                    psiFile as SmkFile
            )
            SMK_VARS_CHECKPOINTS -> SmkCheckPointsType(
                    PsiTreeUtil.getParentOfType(referenceExpression, SmkCheckPoint::class.java),
                    psiFile as SmkFile
            )
            else -> null
        }
    }
}