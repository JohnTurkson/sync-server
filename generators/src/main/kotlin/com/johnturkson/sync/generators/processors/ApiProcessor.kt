package com.johnturkson.sync.generators.processors

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.johnturkson.sync.generators.annotations.apigateway.Route

@KspExperimental
class ApiProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: Map<String, String>,
) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val annotation = requireNotNull(Route::class.qualifiedName)
        val classDeclarations = resolver.getSymbolsWithAnnotation(annotation)
            .filterIsInstance<KSClassDeclaration>()
        
        val routes = classDeclarations.map { classDeclaration ->
            val route = classDeclaration.getAnnotationsByType(Route::class).first()
            classDeclaration to route
        }.toMap()
        
        if (routes.isNotEmpty()) generateApi(routes)
        
        return emptyList()
    }
    
    private fun generateApi(routes: Map<KSClassDeclaration, Route>) {
        val location = requireNotNull(options["location"])
        val hostedZone = requireNotNull(options["HOSTED_ZONE"])
        val apiEndpoint = requireNotNull(options["API_ENDPOINT"])
        
        val generatedClassName = "Api"
        val generatedPackageName = requireNotNull(options["location"]) + ".apis." + hostedZone
        
        val imports = mutableSetOf(
            "import software.amazon.awscdk.services.apigatewayv2.alpha.AddRoutesOptions",
            "import software.amazon.awscdk.services.apigatewayv2.alpha.DomainMappingOptions",
            "import software.amazon.awscdk.services.apigatewayv2.alpha.DomainName",
            "import software.amazon.awscdk.services.apigatewayv2.alpha.HttpApi",
            "import software.amazon.awscdk.services.apigatewayv2.alpha.HttpApiProps",
            "import software.amazon.awscdk.services.apigatewayv2.alpha.HttpMethod",
            "import software.amazon.awscdk.services.apigatewayv2.integrations.alpha.HttpLambdaIntegration",
            "import software.amazon.awscdk.services.certificatemanager.DnsValidatedCertificate",
            "import software.amazon.awscdk.services.certificatemanager.DnsValidatedCertificateProps",
            "import software.amazon.awscdk.services.route53.ARecord",
            "import software.amazon.awscdk.services.route53.HostedZone",
            "import software.amazon.awscdk.services.route53.HostedZoneProviderProps",
            "import software.amazon.awscdk.services.route53.RecordTarget",
            "import software.amazon.awscdk.services.route53.targets.ApiGatewayv2DomainProperties",
            "import software.constructs.Construct"
        )
        
        routes.forEach { (handler, _) ->
            imports += "import $location.functions.${handler.simpleName.asString()}"
        }
        
        val generatedRoutes = routes.map { (handler, route) ->
            """
                AddRoutesOptions.builder()
                    .path("${route.path}")
                    .methods(listOf(HttpMethod.${route.method}))
                    .integration(HttpLambdaIntegration("${handler.simpleName.asString()}Integration", ${handler.simpleName.asString()}.build(construct)))
                    .build()
            """.trimIndent()
        }.joinToString(separator = "\n\n", prefix = "buildList {\n", postfix = "}\n") { generated ->
            """
                add(
                    $generated
                )
            """.trimIndent()
        }
        
        val generatedObject = """
            package $generatedPackageName
            
            ${imports.sorted().joinToString(separator = "\n")}
            
            object $generatedClassName {
                private lateinit var instance: HttpApi
                
                fun build(construct: Construct): HttpApi {
                    if ($generatedClassName::instance.isInitialized) return instance
                    
                    val hostedZone = HostedZone.fromLookup(
                        construct,
                        "HostedZone[$hostedZone]",
                        HostedZoneProviderProps.builder()
                            .domainName("$hostedZone")
                            .build()
                    )
                    
                    val certificate = DnsValidatedCertificate(
                        construct,
                        "Certificate[$apiEndpoint]",
                        DnsValidatedCertificateProps.builder()
                            .domainName("$apiEndpoint")
                            .hostedZone(hostedZone)
                            .build()
                    )
                    
                    val domainName = DomainName.Builder.create(construct, "DomainName[$apiEndpoint]")
                        .domainName("$apiEndpoint")
                        .certificate(certificate)
                        .build()
                    
                    val apiDomainProperties = ApiGatewayv2DomainProperties(
                        domainName.regionalDomainName,
                        domainName.regionalHostedZoneId
                    )
                    
                    val domainMappingOptions = DomainMappingOptions.builder()
                        .domainName(domainName)
                        .build()
                    
                    val api = HttpApi(
                        construct,
                        "HttpApi[$apiEndpoint]",
                        HttpApiProps.builder()
                            .disableExecuteApiEndpoint(true)
                            .defaultDomainMapping(domainMappingOptions)
                            .build()
                    )
                    
                    val routes = $generatedRoutes
                    routes.forEach { route -> api.addRoutes(route) }
                    
                    ARecord.Builder.create(construct, "ARecord[$apiEndpoint]")
                        .recordName("$apiEndpoint")
                        .zone(hostedZone)
                        .target(RecordTarget.fromAlias(apiDomainProperties))
                        .build()
                    
                    instance = api
                    
                    return instance
                }
            }
        """.trimIndent()
        
        val generatedFile = codeGenerator.createNewFile(
            Dependencies.ALL_FILES,
            generatedPackageName,
            generatedClassName,
            "kt"
        )
        
        generatedFile.bufferedWriter().use { writer -> writer.write(generatedObject) }
    }
}
