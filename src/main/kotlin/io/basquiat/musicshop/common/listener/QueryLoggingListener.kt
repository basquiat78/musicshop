package io.basquiat.musicshop.common.listener

import io.basquiat.musicshop.common.utils.logger
import io.r2dbc.proxy.core.QueryExecutionInfo
import io.r2dbc.proxy.listener.ProxyExecutionListener
import io.r2dbc.proxy.support.QueryExecutionInfoFormatter

class QueryLoggingListener: ProxyExecutionListener {

    private val log = logger<QueryLoggingListener>()

    override fun afterQuery(execInfo: QueryExecutionInfo) {
        val formatter = QueryExecutionInfoFormatter()
        formatter.addConsumer { info, sb ->
            sb.append("ConnectionId: ")
            sb.append(info.connectionInfo.connectionId)
        }
        formatter.newLine()
        formatter.showQuery()
        formatter.newLine()
        formatter.showBindings()
        formatter.newLine()
        formatter.addConsumer { info, sb ->
            sb.append("Result Count : ")
            sb.append(info.currentResultCount)
        }
        log.info(execInfo.let { formatter.format(it) })
    }

    override fun eachQueryResult(execInfo: QueryExecutionInfo) {
        val formatter = QueryExecutionInfoFormatter()
        formatter.addConsumer { info, sb ->
            sb.append("Result Row : ")
            sb.append(info.currentMappedResult)
        }
        log.info(formatter.format(execInfo))
    }
}