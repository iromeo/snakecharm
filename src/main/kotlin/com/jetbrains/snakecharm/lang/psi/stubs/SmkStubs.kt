package com.jetbrains.snakecharm.lang.psi.stubs

import com.intellij.psi.stubs.NamedStub
import com.jetbrains.snakecharm.lang.psi.SmkCheckPoint
import com.jetbrains.snakecharm.lang.psi.SmkRule
import com.jetbrains.snakecharm.lang.psi.SmkSubworkflow

interface SmkRuleStub: NamedStub<SmkRule>
interface SmkCheckpointStub: NamedStub<SmkCheckPoint>
interface SmkSubworkflowStub: NamedStub<SmkSubworkflow>
