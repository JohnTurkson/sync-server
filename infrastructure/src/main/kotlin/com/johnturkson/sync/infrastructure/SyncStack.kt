package com.johnturkson.sync.infrastructure

import software.amazon.awscdk.Duration
import software.amazon.awscdk.Stack
import software.amazon.awscdk.StackProps
import software.amazon.awscdk.services.dynamodb.Attribute
import software.amazon.awscdk.services.dynamodb.AttributeType
import software.amazon.awscdk.services.dynamodb.BillingMode
import software.amazon.awscdk.services.dynamodb.GlobalSecondaryIndexProps
import software.amazon.awscdk.services.dynamodb.LocalSecondaryIndexProps
import software.amazon.awscdk.services.dynamodb.ProjectionType
import software.amazon.awscdk.services.dynamodb.Table
import software.amazon.awscdk.services.lambda.Code
import software.amazon.awscdk.services.lambda.Function
import software.amazon.awscdk.services.lambda.Runtime
import software.constructs.Construct

class SyncStack(parent: Construct, name: String, props: StackProps? = null) : Stack(parent, name, props) {
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
        
        Function.Builder.create(this, "TestFunction2")
            .functionName("TestFunction2")
            .description("test")
            .code(Code.fromAsset("../handlers/build/lambda/image/handlers.zip"))
            .handler("com.johnturkson.sync.handlers.functions.CreateUserFunction")
            .timeout(Duration.seconds(5))
            .memorySize(1024)
            .runtime(Runtime.PROVIDED_AL2)
            .build()
    }
}
