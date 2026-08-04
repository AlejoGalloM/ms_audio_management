# ms_audio_management

## Resumen del microservicio
Este microservicio recibe audios, los almacena en S3, dispara la transcripción en AWS Transcribe y entrega el resultado al cliente.  
El cliente puede operar por HTTP reactivo o por WebSocket (incluyendo carga fragmentada del audio comprimido).

## Tabla de Arquitectura por Capas
| Capa | Módulos | Responsabilidad |
|---|---|---|
| Domain | `domain/model`, `domain/usecase` | Contratos (puertos) y orquestación del flujo de transcripción |
| Entry Points | `infrastructure/entry-points/reactive-web`, `infrastructure/entry-points/web-socket` | Exponer rutas HTTP y canal WebSocket para comandos de negocio |
| Driven Adapters | `infrastructure/driven-adapters/s3-repository`, `dynamo-db`, `transcribe` | Integración con AWS S3, DynamoDB y AWS Transcribe |
| Helpers | `infrastructure/helpers/extract-data`, `zip-decode`, `exceptions`, `metrics` | Utilidades transversales: parseo JSON, descompresión, errores técnicos y métricas |
| Application | `applications/app-service` | Ensamble Spring Boot y arranque del servicio |

## Tabla de Dependencias Core
| Tecnología | Uso en el microservicio |
|---|---|
| Spring Boot WebFlux | Endpoints reactivos HTTP y WebSocket |
| Project Reactor (`Mono`/`Flux`) | Composición no bloqueante de flujos |
| AWS S3 SDK Async | Subida y consulta de archivos |
| AWS DynamoDB SDK Async | Persistencia y consulta de transcripciones |
| AWS Transcribe SDK | Inicio y consulta de jobs de transcripción |
| Micrometer + Prometheus | Exposición de métricas |
| Jacoco + PIT | Cobertura y mutation testing |

## Reglas de Negocio
1. El cliente puede subir audio fragmentado por WebSocket (`upload`), el backend ensambla, decodifica Base64 y descomprime GZIP.
2. El audio final se sube a S3 con el nombre lógico recibido.
3. Para iniciar transcripción (`transcript` o `GET /transcribe/{audioName}`), el use case genera un `jobId` aleatorio.
4. Se dispara AWS Transcribe con nombre `transcription{jobId}` sobre `s3://bucket/{audioName}`.
5. Se guarda en DynamoDB un registro inicial con `jobId`, `fileName` y `transcript=""`.
6. Para consultar estado (`status` o `GET /transcribestatus/{jobId}`) se lee el estado del job en Transcribe.
7. Para obtener resultado (`obtain` o `GET /gettranscribe/{jobId}`), si Dynamo ya tiene `transcript` lo retorna.
8. Si Dynamo aún tiene `transcript` vacío, se lee `transcription{jobId}.json` desde S3, se extrae `/results/transcripts/0/transcript`, se actualiza Dynamo y se responde.

## Instrucciones de Ejecución Local
### Pre-requisitos
- Java 17
- Gradle Wrapper (`./gradlew`)
- AWS profile local `default`
- Emuladores/servicios locales para endpoints configurados:
  - S3/Transcribe: `http://localhost:4566`
  - DynamoDB: `http://localhost:8010`
- Bucket y tabla esperados:
  - Bucket: `mytranscribebucketexample`
  - Tabla: `audio-recorder`

### Comandos
```bash
./gradlew clean build
./gradlew bootRun --args='--spring.profiles.active=local'
```

## Catálogo de Errores Técnicos
| Código | Nombre | Descripción |
|---|---|---|
| AMT0001 | Bad Request | Missing Information in the request |
| AMT0002 | Technical Exception | An error occurred while interacting with S3 |
| N/D | S3_UPLOAD_ERROR | Constante declarada sin metadatos (código/mensaje/descr.) en enum |
| N/D | S3_PUT_OBJECT_FAIL | Constante declarada sin metadatos (código/mensaje/descr.) en enum |
| N/D | TECHNICAL_DYNAMODB_EXCEPTION | Constante declarada sin metadatos (código/mensaje/descr.) en enum |

## Tópicos Kafka Configurados
No se encontraron consumidores, productores ni configuración de tópicos Kafka en el código o `application*.yaml`.

| Cluster | Topic | Variable/Propiedad | Estado |
|---|---|---|---|
| N/A | N/A | N/A | No configurado |

## Pipeline de Procesamiento
```text
Cliente
  | WebSocket(upload fragmentos) o HTTP/WebSocket(transcript/status/obtain)
  v
Entry Point (Handler / WebSocketTranscriptionHandler)
  v
AudioUseCase
  |-- uploadAudio --> S3Adapter.uploadFile --> S3
  |-- transcribe --> TranscribeAdapter.startJob --> AWS Transcribe
  |                \-> DynamoDB.saveTranscript(transcript="")
  |-- getTranscribeStatus --> TranscribeAdapter.getJobStatus
  \-- getTranscribe
       |-- si transcript en Dynamo != "" -> responder
       \-- si transcript vacío
            -> S3 transcription{jobId}.json
            -> ExtractData (/results/transcripts/0/transcript)
            -> DynamoDB.updateTranscript
            -> responder
```

## Gotchas y Consideraciones Importantes
| Caso | Comportamiento actual |
|---|---|
| Fragmentos incompletos | Se conserva estado en memoria (`fragmentosCache`) hasta completar |
| Reinicio de instancia | Se pierde `fragmentosCache` (no persistente/distribuido) |
| Errores en descompresión o parseo | Se responde mensaje de error al cliente WebSocket |
| Generación de `jobId` | Aleatorio en memoria; no hay estrategia explícita anti-colisión |
| Llaves S3 | Se construyen como `bucketName/objectKey` además del bucket; revisar convención operativa |
| Retries lectura transcript S3 | Hasta 5 intentos con delay de 1 minuto |

## Convenciones del Proyecto
| Convención | Estado observado |
|---|---|
| Commits | No se encontró validación automática del formato de commit en este repositorio |
| Cobertura | Integración con Jacoco y PIT (agregación multi-módulo) |
| Logging | Uso de `Slf4j` con logs de info/error en puntos críticos |
| Reactividad | Uso extensivo de `Mono`; no se observó `.block()` en código productivo |
| Inmutabilidad | Se favorece `toBuilder()` en actualización de `Audio`, aunque el modelo mantiene setters |

