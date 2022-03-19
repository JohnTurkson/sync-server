package com.johnturkson.sync.cdk

import com.johnturkson.sync.common.generated.Tables
import com.johnturkson.sync.functions.generated.Functions
import software.amazon.awscdk.Stack
import software.constructs.Construct

class SyncStack(parent: Construct, name: String) : Stack(parent, name) {
    init {
        val tables = Tables.build(this)
        val functions = Functions.build(this)
        
        tables.forEach { table ->
            functions.forEach { function ->
                table.grantReadWriteData(function)
            }
        }
    }
}
