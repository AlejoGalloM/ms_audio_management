# Patrones y guía de código

## Patrón Ports & Adapters
| Puerto (Domain) | Adaptador (Infraestructura) | Implementación |
|---|---|---|
| `TranscribeGateway` | Driven Adapter Transcribe | `Transcribe` |
| `S3Gateway` | Driven Adapter S3 | `S3Adapter` + `S3Operations` |
| `DynamoGateway` | Driven Adapter DynamoDB | `DynamoDBTemplateAdapter` |
| `ZipDecodeGateway` | Helper Adapter | `ZipDecode` |

### Cómo agregar un puerto nuevo (paso a paso)
1. Crear interfaz en `domain/model` (contrato del dominio, sin SDK externos).
2. Inyectar el puerto en el UseCase y usarlo desde la lógica de negocio.
3. Implementar adaptador en `infrastructure/driven-adapters/*` con detalles técnicos.
4. Registrar/configurar beans necesarios en Spring.
5. Cubrir con pruebas de use case (mock del puerto) y pruebas del adaptador.

## Patrón Command Object
Actualmente el entry point llama al use case con parámetros sueltos (`String audioName`, `String jobId`).  
Para desacoplar aún más transporte y negocio, se recomienda encapsular cada intención en un Command.

```java
public record StartTranscriptionCommand(String audioName) {}

public Mono<String> transcribe(StartTranscriptionCommand command) {
    return transcribeGateway.transcribe(generateJobId(), command.audioName());
}
```

Esto evita depender de `ServerRequest` o formato de mensaje WebSocket dentro del caso de uso.

## Reactive Streams
| Operador Reactor | Uso en el proyecto |
|---|---|
| `Mono.just` | Respuestas inmediatas y wrapping de valores |
| `Mono.fromCallable` | Trabajo diferido para transformaciones puntuales |
| `Mono.fromFuture` | Adaptación de SDK Async de AWS |
| `flatMap` | Encadenar llamadas asíncronas dependientes |
| `map` | Transformación de payload |
| `then` / `thenReturn` | Ignorar resultado previo y continuar flujo |
| `onErrorResume` | Mapeo de errores técnicos (S3) |
| `retryWhen` | Reintentos controlados en lectura S3 |
| `Mono.empty` | Ausencia de item al consultar Dynamo |

**Regla:** está prohibido usar `.block()` en flujos productivos reactivos.

## Manejo de Poison Pills / DLQ
El servicio no implementa Kafka ni DLQ actualmente. El árbol real de decisión de entrada es:

```text
Mensaje entra por WebSocket
  ├─ comando desconocido -> responder "Comando no válido"
  ├─ upload mal formado/parseo falla -> responder "Error procesando fragmento"
  ├─ descompresión/decodificación falla -> responder "Error procesando archivo"
  └─ éxito -> continuar flujo de negocio
```

Si se agrega Kafka en el futuro, se recomienda:
- JSON inválido -> enviar a DLQ
- metadata nula -> rechazar y enviar a DLQ
- fallo downstream transitorio -> retry con backoff
- fallo downstream no recuperable -> DLQ

## Decisiones de Extracción de Datos
No existe lógica por tipo de cluster (`MNT` vs `NMNT`) ni campo `identificationTx` en el código actual.

| Contexto | Path JSON encontrado | Estado |
|---|---|---|
| `identificationTx` para cluster MNT | N/A | No implementado |
| `identificationTx` para cluster NMNT | N/A | No implementado |
| Texto de transcripción AWS | `/results/transcripts/0/transcript` | Implementado en `ExtractData` |

## Construcción de Llaves de Deduplicación
No se encontraron estructuras `DedupKey`, hashes de deduplicación ni validación de duplicados en el flujo actual.

| Tipo de llave | Formato exacto | Estado |
|---|---|---|
| DedupKey de evento | N/A | No implementado |
| Job de transcripción | `transcription{jobId}` | Implementado |
| Archivo de salida transcript | `transcription{jobId}.json` | Implementado |
| Registro Dynamo | `jobId` numérico | Implementado |

