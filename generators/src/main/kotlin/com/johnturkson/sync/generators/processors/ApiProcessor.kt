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
import com.johnturkson.sync.generators.annotations.apigateway.HttpApiRoute
import com.johnturkson.sync.generators.annotations.apigateway.WebsocketApiRoute

@KspExperimental
class ApiProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: Map<String, String>,
) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val httpApiAnnotation = requireNotNull(HttpApiRoute::class.qualifiedName)
        val websocketApiAnnotation = requireNotNull(WebsocketApiRoute::class.qualifiedName)
        
        val httpApiClassDeclarations = resolver.getSymbolsWithAnnotation(httpApiAnnotation)
            .filterIsInstance<KSClassDeclaration>()
        val websocketApiClassDeclarations = resolver.getSymbolsWithAnnotation(websocketApiAnnotation)
            .filterIsInstance<KSClassDeclaration>()
        
        val httpRoutes = httpApiClassDeclarations.map { classDeclaration ->
            val route = classDeclaration.getAnnotationsByType(HttpApiRoute::class).first()
            route to classDeclaration
        }.groupBy { (route, _) ->
            route.endpoint
        }
        
        val websocketRoutes = websocketApiClassDeclarations.map { classDeclaration ->
            val route = classDeclaration.getAnnotationsByType(WebsocketApiRoute::class).first()
            route to classDeclaration
        }.groupBy { (route, _) ->
            route.endpoint
        }
        
        if (httpRoutes.isNotEmpty() || websocketRoutes.isNotEmpty()) generateHostedZone()
        if (httpRoutes.isNotEmpty()) generateHttpApis(httpRoutes)
        if (websocketRoutes.isNotEmpty()) generateWebsocketApis(websocketRoutes)
        
        return emptyList()
    }
    
    private fun generateHttpApis(apis: Map<String, List<Pair<HttpApiRoute, KSClassDeclaration>>>) {
        val location = requireNotNull(options["location"])
        val generatedClassName = "HttpApis"
        val generatedPackageName = "$location.apis"
        
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
            "import software.amazon.awscdk.services.lambda.Function",
            "import software.amazon.awscdk.services.route53.ARecord",
            "import software.amazon.awscdk.services.route53.RecordTarget",
            "import software.amazon.awscdk.services.route53.targets.ApiGatewayv2DomainProperties",
            "import software.constructs.Construct",
        )
        
        val generatedApis = apis.map { (endpoint, routes) ->
            val generatedRoutes = routes.map { (route, handler) ->
                route to handler.simpleName.asString()
            }.onEach { (_, handler) ->
                imports += "import $location.functions.$handler"
            }.joinToString(separator = "\n") { (route, handler) ->
                Triple(route.method, route.route, handler)
                """Triple("${route.method}", "${route.route}", $handler.build(construct)),""".prependIndent("    ")
            }
            
            """
                buildHttpApi(construct, "$endpoint", listOf(
                $generatedRoutes
                ))
            """.trimIndent()
        }
        
        val generatedObject = """
            package $generatedPackageName
            
            ${imports.sorted().joinToString("\n")}
            
            object HttpApis {
                private lateinit var instance: List<HttpApi>
                
                fun build(construct: Construct): List<HttpApi> {
                    if (::instance.isInitialized) return instance
                    instance = listOf(
                    ${generatedApis.joinToString(",\n") { route -> route.prependIndent("    ") }}
                    )
                    return instance
                }
                
                private fun buildHttpApi(construct: Construct, endpoint: String, routes: List<Triple<String, String, Function>>): HttpApi {
                    val hostedZone = HostedZone.build(construct)
                    val certificate = DnsValidatedCertificate(
                        construct,
                        "Certificate[${"\$endpoint"}]",
                        DnsValidatedCertificateProps.builder()
                            .domainName(endpoint)
                            .hostedZone(hostedZone)
                            .build()
                    )
                    val domainName = DomainName.Builder.create(construct, "DomainName[${"\$endpoint"}]")
                        .domainName(endpoint)
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
                        "HttpApi[${"\$endpoint"}]",
                        HttpApiProps.builder()
                            .apiName(endpoint)
                            .disableExecuteApiEndpoint(true)
                            .defaultDomainMapping(domainMappingOptions)
                            .build()
                    )
                    ARecord.Builder.create(construct, "ARecord[${"\$endpoint"}]")
                        .recordName(endpoint)
                        .zone(hostedZone)
                        .target(RecordTarget.fromAlias(apiDomainProperties))
                        .build()
                    routes.forEach { (method, route, handler) ->
                        api.addRoutes(AddRoutesOptions.builder()
                            .methods(listOf(HttpMethod.valueOf(method)))
                            .path(route)
                            .integration(HttpLambdaIntegration(endpoint + route, handler))
                            .build())
                    }
                    return api
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
    
    private fun generateWebsocketApis(apis: Map<String, List<Pair<WebsocketApiRoute, KSClassDeclaration>>>) {
        val location = requireNotNull(options["location"])
        val routeSelectionExpression = "\\\$" + requireNotNull(options["routeSelectionExpression"]).removePrefix("\$")
        val generatedClassName = "WebSocketApis"
        val generatedPackageName = "$location.apis"
        
        val imports = mutableSetOf(
            "import software.amazon.awscdk.services.apigatewayv2.alpha.WebSocketApi",
            "import software.amazon.awscdk.services.apigatewayv2.alpha.WebSocketRouteOptions",
            "import software.amazon.awscdk.services.apigatewayv2.integrations.alpha.WebSocketLambdaIntegration",
            "import software.amazon.awscdk.services.lambda.Function",
            "import software.constructs.Construct",
        )
        
        val generatedApis = apis.map { (endpoint, routes) ->
            val generatedRoutes = routes.map { (route, handler) ->
                route.route to handler.simpleName.asString()
            }.onEach { (_, handler) ->
                imports += "import $location.functions.$handler"
            }.joinToString(separator = "\n") { (route, handler) ->
                """Pair("$route", $handler.build(construct)),""".prependIndent("    ")
            }
            
            """
                buildWebSocketApi(construct, "$endpoint", listOf(
                $generatedRoutes
                ))
            """.trimIndent()
        }
        
        val generatedObject = """
            package $generatedPackageName
            
            ${imports.sorted().joinToString("\n")}
            
            object WebSocketApis {
                private lateinit var instance: List<WebSocketApi>
                
                fun build(construct: Construct): List<WebSocketApi> {
                    if (::instance.isInitialized) return instance
                    instance = listOf(
                    ${generatedApis.joinToString(",\n") { route -> route.prependIndent("    ") }}
                    )
                    return instance
                }
                
                private fun buildWebSocketApi(construct: Construct, endpoint: String, routes: List<Pair<String, Function>>): WebSocketApi {
                    val api = WebSocketApi.Builder.create(construct, endpoint)
                        .apiName(endpoint)
                        .routeSelectionExpression("$routeSelectionExpression")
                        .build()
                    routes.forEach { (route, handler) ->
                        api.addRoute(route, WebSocketRouteOptions.builder()
                            .integration(WebSocketLambdaIntegration(endpoint + route, handler))
                            .build())
                        api.grantManageConnections(handler)
                    }
                    return api
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
    
    private fun generateHostedZone() {
        val location = requireNotNull(options["location"])
        val domain = requireNotNull(options["hostedZone"])
        val generatedClassName = "HostedZone"
        val generatedPackageName = "$location.apis"
        
        val imports = """
            import software.amazon.awscdk.services.route53.HostedZone
            import software.amazon.awscdk.services.route53.HostedZoneProviderProps
            import software.amazon.awscdk.services.route53.IHostedZone
            import software.constructs.Construct
        """.trimIndent()
        
        val generatedObject = """
            package $generatedPackageName
            
            $imports
            
            object HostedZone {
                private lateinit var instance: IHostedZone
                
                fun build(construct: Construct): IHostedZone {
                    if (::instance.isInitialized) return instance
                    instance = HostedZone.fromLookup(
                        construct,
                        "HostedZone[$domain]",
                        HostedZoneProviderProps.builder()
                            .domainName("$domain")
                            .build()
                    )
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
