package com.johnturkson.sync.cdk

import com.johnturkson.sync.common.generated.Tables
import com.johnturkson.sync.functions.generated.Functions
import software.amazon.awscdk.Stack
import software.amazon.awscdk.StackProps
import software.amazon.awscdk.services.apigatewayv2.alpha.AddRoutesOptions
import software.amazon.awscdk.services.apigatewayv2.alpha.DomainMappingOptions
import software.amazon.awscdk.services.apigatewayv2.alpha.DomainName
import software.amazon.awscdk.services.apigatewayv2.alpha.HttpApi
import software.amazon.awscdk.services.apigatewayv2.alpha.HttpApiProps
import software.amazon.awscdk.services.apigatewayv2.alpha.HttpMethod
import software.amazon.awscdk.services.apigatewayv2.integrations.alpha.HttpLambdaIntegration
import software.amazon.awscdk.services.certificatemanager.DnsValidatedCertificate
import software.amazon.awscdk.services.certificatemanager.DnsValidatedCertificateProps
import software.amazon.awscdk.services.route53.ARecord
import software.amazon.awscdk.services.route53.HostedZone
import software.amazon.awscdk.services.route53.HostedZoneProviderProps
import software.amazon.awscdk.services.route53.RecordTarget
import software.amazon.awscdk.services.route53.targets.ApiGatewayv2DomainProperties
import software.constructs.Construct

class SyncStack(
    parent: Construct,
    name: String,
    props: StackProps? = null,
) : Stack(parent, name, props) {
    init {
        val tables = Tables.build(this)
        val functions = Functions.build(this)
        
        tables.forEach { table ->
            functions.forEach { function ->
                table.grantReadWriteData(function)
            }
        }
        
        val hostedZone = HostedZone.fromLookup(
            this,
            "HostedZone",
            HostedZoneProviderProps.builder()
                .domainName("johnturkson.com")
                .build()
        )
        
        val certificate = DnsValidatedCertificate(
            this,
            "Certificate",
            DnsValidatedCertificateProps.builder()
                .domainName("johnturkson.com")
                .hostedZone(hostedZone)
                .build()
        )
        
        val domainName = DomainName.Builder.create(this, "DomainName")
            .domainName("johnturkson.com")
            .certificate(certificate)
            .build()
        
        val apiDomainProperties = ApiGatewayv2DomainProperties(
            domainName.regionalDomainName,
            domainName.regionalHostedZoneId
        )
        
        val aRecord = ARecord.Builder.create(this, "ARecord")
            .zone(hostedZone)
            .target(RecordTarget.fromAlias(apiDomainProperties))
            .build()
        
        val api = HttpApi(this, "Api", HttpApiProps.builder()
            .disableExecuteApiEndpoint(true)
            .defaultDomainMapping(
                DomainMappingOptions.builder().domainName(domainName).build()
            )
            .build())
        
        val routes = functions.mapIndexed { index, function ->
            AddRoutesOptions.builder()
                .path("/$index")
                .methods(listOf(HttpMethod.GET))
                .integration(HttpLambdaIntegration("LambdaIntegration$index", function))
                .build()
        }
        
        routes.forEach { route -> api.addRoutes(route) }
    }
}
