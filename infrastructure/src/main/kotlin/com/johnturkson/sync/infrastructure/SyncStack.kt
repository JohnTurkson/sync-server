package com.johnturkson.sync.infrastructure

import com.johnturkson.sync.handlers.generated.Functions
import software.amazon.awscdk.Stack
import software.amazon.awscdk.services.dynamodb.Attribute
import software.amazon.awscdk.services.dynamodb.AttributeType
import software.amazon.awscdk.services.dynamodb.BillingMode
import software.amazon.awscdk.services.dynamodb.GlobalSecondaryIndexProps
import software.amazon.awscdk.services.dynamodb.LocalSecondaryIndexProps
import software.amazon.awscdk.services.dynamodb.ProjectionType
import software.amazon.awscdk.services.dynamodb.Table
import software.constructs.Construct

class SyncStack(parent: Construct, name: String) : Stack(parent, name) {
    init {
        val table = Table.Builder.create(this, "TestTable2")
            .tableName("TestTable2")
            .billingMode(BillingMode.PAY_PER_REQUEST)
            .partitionKey(Attribute.builder().type(AttributeType.STRING).name("primary").build())
            .sortKey(Attribute.builder().type(AttributeType.STRING).name("secondary").build())
            .build()
        
        val lsi = LocalSecondaryIndexProps.builder()
            .indexName("lsi")
            .sortKey(Attribute.builder().type(AttributeType.STRING).name("secondary").build())
            .projectionType(ProjectionType.ALL)
            .build()
        
        val gsi = GlobalSecondaryIndexProps.builder().indexName("gsi")
            .partitionKey(Attribute.builder().type(AttributeType.STRING).name("primary").build())
            .sortKey(Attribute.builder().type(AttributeType.STRING).name("secondary").build())
            .projectionType(ProjectionType.ALL)
            .build()
        
        table.addGlobalSecondaryIndex(gsi)
        table.addLocalSecondaryIndex(lsi)
        
        Functions.build(this)
    }
}
