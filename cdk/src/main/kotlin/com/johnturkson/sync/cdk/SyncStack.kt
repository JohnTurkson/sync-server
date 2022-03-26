package com.johnturkson.sync.cdk

import com.johnturkson.sync.common.generated.Tables
import com.johnturkson.sync.functions.generated.apis.HttpApis
import com.johnturkson.sync.functions.generated.functions.Functions
import software.amazon.awscdk.Stack
import software.amazon.awscdk.StackProps
import software.constructs.Construct

class SyncStack(
    parent: Construct,
    name: String,
    props: StackProps? = null,
) : Stack(parent, name, props) {
    init {
        val tables = Tables.build(this)
        val functions = Functions.build(this)
        val httpApis = HttpApis.build(this)

        tables.forEach { table ->
            functions.forEach { function ->
                table.grantReadWriteData(function)
            }
        }
    }
}
