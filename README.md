# Booking Service

Сервис бронирования временных интервалов. Проверяет пересечение диапазонов (`existing.start < requested.end && existing.end > requested.start`), не считает смежные слоты конфликтом и поддерживает отмену.

## API
- `POST /api/bookings` — resourceId, customerId, startsAt, endsAt (ISO-8601)
- `POST /api/bookings/{id}/cancel`

## Проверки
`mvn verify` — тесты конфликтующих и смежных интервалов, а также проверки границ.

Стек: Java 21 · Spring Boot · Spring Data JPA · PostgreSQL/H2 · транзакционный сервис · JUnit 5 · Mockito.

Для production следует дополнить прикладную проверку ограничением от гонок на уровне БД (например, PostgreSQL exclusion constraint для временных диапазонов).
