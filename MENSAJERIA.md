# Mensajería asíncrona en pedidos

Pedidos es el **productor** del sistema. Cada vez que se crea un pedido
(`POST /api/pedidos`), publica el evento en RabbitMQ, una vez por caso de uso.
Los consumidores reaccionan por su cuenta, sin que pedidos espere su respuesta:

| Exchange | Tipo | Routing key | Cola | Consumidor | Qué hace |
|---|---|---|---|---|---|
| `tecnomarket.pedidos.direct` | direct | `pedido.stock` | `q.stock` | productos | Descuenta el stock |
| `tecnomarket.pedidos.direct` | direct | `pedido.ticket` | `q.ticket` | mensajería | Genera el ticket |
| `tecnomarket.eventos.topic` | topic | `pedido.creado` | `q.notificaciones` (`pedido.#`) | mensajería | Notifica |

**Por qué dos tipos de exchange.** El *direct* entrega cada routing key a una
sola cola: sirve para mensajes dirigidos a un servicio concreto. El *topic*
entrega por patrón: cualquier servicio nuevo puede suscribirse a `pedido.#`
sin cambiar el código de pedidos.

## Flujo

```
PedidoController ──> PedidoService ──> PedidoRepository (guarda en MySQL)
                           │
                           └──> PedidoEventPublisher ──> exchanges direct y topic ──> colas
```

## Separación de responsabilidades

| Clase | Responsabilidad |
|---|---|
| `controller/PedidoController` | Solo HTTP: valida la petición y responde |
| `service/PedidoService` | Negocio: arma el pedido, lo guarda y pide publicar el evento |
| `messaging/producer/PedidoEventPublisher` | Única clase que habla con RabbitMQ |
| `messaging/event/PedidoCreadoEvent` | Contrato del mensaje (JSON) |
| `config/TecnomarketRabbitProperties` | Nombres leídos desde `application.yml` |
| `config/RabbitTopologyConfig` | Beans de los dos exchanges en los que publica |
| `config/RabbitMessagingConfig` | Conversor JSON y activación de las propiedades |

## Configuración centralizada

Todos los nombres están en el bloque `tecnomarket.rabbit` de `application.yml`.
Ninguna clase escribe un nombre de exchange o routing key a mano.

| Variable de entorno | Uso | Valor por defecto |
|---|---|---|
| `RABBIT_ADDRESSES` | Nodos del clúster | `localhost:5672,localhost:5673` |
| `RABBIT_USER` / `RABBIT_PASS` | Credenciales | `guest` / `guest` |

## Manejo de errores al publicar

- **Confirmación del broker** (`publisher-confirm-type: correlated`): si
  RabbitMQ rechaza el mensaje, queda en el log con el id del pedido.
- **Mensaje sin destino** (`publisher-returns` + `mandatory`): si ninguna cola
  está enlazada con esa routing key, el broker devuelve el mensaje y se
  registra un aviso. Sirve para detectar nombres mal escritos.
- **Broker caído**: se registra el error y la petición responde igual `201`,
  porque el pedido ya quedó guardado. Mejora futura: patrón *outbox*.
- **messageId único** (UUID) en cada mensaje, para que los consumidores
  detecten duplicados.

## Pruebas

Los tests no necesitan RabbitMQ: el publicador se simula con Mockito.

- `PedidoServiceTest`: guarda primero y publica después.
- `PedidoEventPublisherTest`: publica las tres routing keys con los nombres
  del YAML y no falla si el broker está caído.
- `PedidoSecurityTest`: un `POST` válido publica el evento y uno inválido no.
