package com.iagothz.mergetd.app

import com.iagothz.mergetd.logic.Match

enum class SessionKind { NEW, CHALLENGE, RESUMED }

data class MatchSession(val match: Match, val previousBest: Int, val kind: SessionKind)
