package com.cyebrcina.pos.data.repository

import com.cyebrcina.pos.data.remote.model.ZReport
import java.time.LocalDate

/** ALL is the combined report; ONLINE is website orders only; TILL is till + Admin test
 * orders only — the two versions requested for closing out online vs till sales separately. */
enum class ReportChannel { ALL, ONLINE, TILL }

interface ReportRepository {
    suspend fun getZReport(date: LocalDate? = null, channel: ReportChannel = ReportChannel.ALL): Result<ZReport>
}
