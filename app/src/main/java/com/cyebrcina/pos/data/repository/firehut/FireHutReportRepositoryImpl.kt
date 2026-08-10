package com.cyebrcina.pos.data.repository.firehut

import com.cyebrcina.pos.data.remote.FireHutDeviceApi
import com.cyebrcina.pos.data.remote.errorMessageOrDefault
import com.cyebrcina.pos.data.remote.model.ZReport
import com.cyebrcina.pos.data.repository.ReportRepository
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json

@Singleton
class FireHutReportRepositoryImpl @Inject constructor(
    private val api: FireHutDeviceApi,
    private val json: Json,
) : ReportRepository {

    override suspend fun getZReport(date: LocalDate?): Result<ZReport> = runCatching {
        val response = api.zReport(date?.format(DateTimeFormatter.ISO_LOCAL_DATE))
        if (!response.isSuccessful) throw IllegalStateException(response.errorMessageOrDefault(json))
        response.body() ?: throw IllegalStateException("Empty response")
    }.recoverCatching { cause ->
        throw if (cause is IOException) IOException("Couldn't reach the server — check your connection", cause) else cause
    }
}
