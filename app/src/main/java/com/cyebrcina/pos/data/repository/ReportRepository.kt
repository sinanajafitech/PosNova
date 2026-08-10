package com.cyebrcina.pos.data.repository

import com.cyebrcina.pos.data.remote.model.ZReport
import java.time.LocalDate

interface ReportRepository {
    suspend fun getZReport(date: LocalDate? = null): Result<ZReport>
}
