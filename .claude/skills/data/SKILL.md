---
name: data
description: Scaffold a new Repository + UseCase + Koin registration for InIndy. Use when adding a new data domain.
argument-hint: <DomainName>
---

# New Repository Scaffold

Scaffold a complete data layer for a new domain using the name in $ARGUMENTS.

## Steps

1. **Create DTO** in `shared/commonMain/data/remote/<domain>/<DomainName>Dto.kt`
   - `@Serializable` data class matching API/Supabase response shape
   - Extension fun `<DomainName>Dto.toDomain(): <DomainName>` for mapping

2. **Create domain model** in `shared/commonMain/domain/model/<DomainName>.kt`
   - Pure Kotlin data class, no framework dependencies

3. **Create Repository interface** in `shared/commonMain/domain/repository/<DomainName>Repository.kt`
   - Suspend functions returning `Result<T>`

4. **Create Repository implementation** in `shared/commonMain/data/repository/<DomainName>RepositoryImpl.kt`
   - Inject `HttpClient` (Ktor) and `<DomainName>Dao` (SQLDelight) via constructor
   - Network-first with local cache fallback
   - Wrap all calls in `runCatching { }`

5. **Create SQLDelight queries** in `shared/commonMain/sqldelight/<DomainName>.sq`
   - Standard CRUD + any domain-specific queries
   - Run `./gradlew :shared:generateSqlDelightInterface` after

6. **Register in Koin** — add interface→impl binding to `shared/commonMain/di/DataModule.kt`

## Supabase conventions
- Table name = snake_case plural of domain name
- Always include `Authorization: Bearer $token` via the Ktor client interceptor
- Geo queries use PostGIS: `ST_DWithin(location, ST_Point($lng, $lat)::geography, $radiusMeters)`
