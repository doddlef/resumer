# Domain + Repository Style

This document summarizes the preferred style from `agent/example.md` for domain model and repository implementation.

## 1) Domain Model Style

- Use immutable Kotlin `data class` for domain entities.
- Keep enums in domain layer (e.g., `AccountStatus`, `AccountRole`).
- Add `fromString(value: String)` in enum companion objects for boundary conversion.
- Domain names use `PascalCase`; properties use `camelCase`.
- Keep domain model independent from persistence framework types.

Short example:

```kotlin
enum class AccountStatus {
    ACTIVE, ARCHIVED, LOCKED;

    companion object {
        fun fromString(value: String): AccountStatus =
            entries.find { it.name.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException("Invalid AccountStatus: $value")
    }
}

data class Account(
    val id: UUID,
    val email: String,
    val password: String,
    val name: String,
    val status: AccountStatus,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
)
```

## 2) Repository Interface Style

- Repository interface belongs to domain boundary.
- Use explicit `selectBy...` methods for reads.
- Use nested query command classes for write inputs (`InsertQuery`, `UpdateQuery`).
- `UpdateQuery` fields are nullable for partial updates.

Short example:

```kotlin
interface AccountRepo {
    fun selectById(id: UUID): Account?
    fun selectByEmail(email: String): Account?

    data class InsertQuery(
        val id: UUID,
        val email: String,
        val password: String,
        val name: String,
    )

    fun insert(query: InsertQuery): Int

    data class UpdateQuery(
        val name: String? = null,
        val status: AccountStatus? = null,
    )
    
    fun updateById(id: UUID, query: UpdateQuery): Int
}
```

## 3) jOOQ Repository Implementation Style

- Keep implementation in infra layer (e.g., `infra.account.JooqAccountRepo`).
- Use `DSLContext` + generated jOOQ tables/POJOs.
- Put mapping conversions in extension functions (`toDomain`, `toJooq`).
- For partial update, build a record, set only provided fields, then update if modified.

Short example:

```kotlin
@Repository
class JooqAccountRepo(private val dsl: DSLContext) : AccountRepo {

    override fun selectById(id: UUID): Account? =
        dsl.selectFrom(ACCOUNTS)
            .where(ACCOUNTS.ID.eq(id))
            .fetchOneInto(P_Accounts::class.java)
            ?.toDomain()

    override fun updateById(id: UUID, query: AccountRepo.UpdateQuery): Int =
        dsl.newRecord(ACCOUNTS).apply {
            query.name?.let { set(ACCOUNTS.NAME, it) }
            query.status?.let { set(ACCOUNTS.STATUS, it.toJooq()) }
        }.let { record ->
            if (record.modified()) dsl.update(ACCOUNTS).set(record).where(ACCOUNTS.ID.eq(id)).execute() else 0
        }
}
```

## 4) Mapping Style

- Add small conversion extensions near the repository implementation.
- Always map enums explicitly between domain and jOOQ enum types.
- Use `requireNotNull` when converting generated nullable POJO fields to non-null domain fields.

Short example:

```kotlin
internal fun E_AccountStatus.toDomain(): AccountStatus = AccountStatus.valueOf(name)
internal fun AccountStatus.toJooq(): E_AccountStatus = E_AccountStatus.valueOf(name)
```

## 5) Practical Guidelines

- Keep comments concise; avoid repeating obvious code intent.
- Prefer clear names and small functions over heavy inline comments.
- Keep repo methods deterministic and side-effect free except DB writes.
- Add caching only where read-hot and invalidation is simple.
