package com.jetbrains.snakecharm.inspections.smksl

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.Ref
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.inspections.SnakemakeInspection
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpoint
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpointArgsSection
import com.jetbrains.snakecharm.lang.psi.SmkWildcardsCollector
import com.jetbrains.snakecharm.stringLanguage.lang.psi.SmkSLReferenceExpressionImpl

class SmkWildcardNotDefinedInspection : SnakemakeInspection() {
    companion object {
        val KEY = Key<HashMap<SmkRuleOrCheckpoint, Ref<List<String>>>>("SmkWildcardNotDefinedInspection_Wildcards")
    }

    override fun buildVisitor(
            holder: ProblemsHolder,
            isOnTheFly: Boolean,
            session: LocalInspectionToolSession
    ) = object : SmkSLInspectionVisitor(holder, session) {

        override fun visitSmkSLReferenceExpression(expr: SmkSLReferenceExpressionImpl) {
            if (!expr.isWildcard()) {
                return
            }

            val ruleOrCheckpointSection = expr.containingRuleOrCheckpointSection() ?: return
            val ruleOrCheckpoint = ruleOrCheckpointSection.getParentRuleOrCheckPoint()

            val wildcardsByRule = session.putUserDataIfAbsent(KEY, hashMapOf())
            if (ruleOrCheckpoint !in wildcardsByRule) {
                val wildcardsDefiningSectionsAvailable = ruleOrCheckpoint.getSections()
                        .asSequence()
                        .filterIsInstance(SmkRuleOrCheckpointArgsSection::class.java)
                        .filter { it.isWildcardsDefiningSection() }.firstOrNull() != null

                val wildcards = when {
                    // if no suitable sections let's think that no wildcards
                    !wildcardsDefiningSectionsAvailable -> emptyList()
                    else -> {
                        // Cannot do via types, we'd like to have wildcards only from
                        // defining sections and ensure that defining sections could be parsed
                        val collector = SmkWildcardsCollector(
                                visitDefiningSections = true,
                                visitExpandingSections = false
                        )
                        ruleOrCheckpoint.accept(collector)
                        collector.getWildcardsNames()
//
//                        val type = TypeEvalContext.codeAnalysis(expr.project, expr.containingFile)
//                                .getType(ruleOrCheckpoint.wildcardsElement)
//
//                        var definedWildcards: List<WildcardDescriptor>? = null
//                        if (type is SmkWildcardsType) {
//                            definedWildcards = type.wildcardsDeclarations
//                                    ?.filter { it.definingSectionRate != UNDEFINED_SECTION }
//                                    ?.ifEmpty { null }
//                        }
//                        definedWildcards
                    }
                }
                wildcardsByRule[ruleOrCheckpoint] =  Ref.create(wildcards)
            }
            val wildcards = wildcardsByRule.getValue(ruleOrCheckpoint).get()
            if (wildcards == null) {
                // failed to parse wildcards defining sections
                registerProblem(
                        expr,
                        SnakemakeBundle.message("INSP.NAME.wildcard.not.defined.cannot.check", expr.text),
                        ProblemHighlightType.WEAK_WARNING
                )
            } else {
                if (expr.text !in wildcards) {
                    val definingSection = ruleOrCheckpoint.getWildcardDefiningSection()?.sectionKeyword
                    val message = when (definingSection) {
                        null -> SnakemakeBundle.message("INSP.NAME.wildcard.not.defined", expr.text)
                        else -> SnakemakeBundle.message(
                                "INSP.NAME.wildcard.not.defined.in.section",
                                expr.text, definingSection
                        )
                    }

                    registerProblem(expr, message)
                }
            }
        }
    }

    override fun getDisplayName(): String = SnakemakeBundle.message("INSP.NAME.wildcard.not.defined", "")
}