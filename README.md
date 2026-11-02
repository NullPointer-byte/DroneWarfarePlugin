# MillitaryTheme

Военно-тематический плагин для Minecraft (Paper/Purpur, **Folia-ready**): управляемые дроны-камикадзе, боевая авиация, стрелковое оружие и обратимые разрушения мира через [FlorestRollback](https://github.com/FlorestMCServer/FlorestRollback).

---

## Возможности

### Дроны (`/drone`)
| Дрон | Носитель | Цена | Особенности |
|---|---|---|---|
| **Shahed** | Phantom | 20 000 | Полёт по взгляду, детонация при столкновении, воронка r=6 |
| **FPV** | Bat (scale 2.5) | 10 000 | Быстрый, манёвренный, зависание по Shift, воронка r=4 |
| **MQ-1 Predator** | Phantom (size 15) | 45 000 | Постоянный полёт, сброс до 8 бомб (ЛКМ), воронка r=18 |

- Управление — камерой игрока (yaw/pitch), пилот «сидит» на сущности.
- Проверка коллизий через `rayTraceBlocks` → детонация.
- Возврат пилота на точку старта после уничтожения.
- Все разрушения логируются в FlorestRollback с тегами (`#Drone`, `#FpvDrone`, `#DroneFlight`).

### Оружие (`/gun`)
| Оружие | Цена | Механика |
|---|---|---|
| Pistol | 1 500 | Одиночный выстрел, урон 6.0 |
| Shotgun | 4 000 | 5 дробин, кулдаун 30 тиков |
| AK | 7 000 | Автоматический огонь (интервал 4 тика) |
| Sniper | 9 000 | Скорость 10.0, урон 9.0, кулдаун 60 тиков |
| RPG | 12 000 | Взрыв + воронка r=10 |
| Flamethrower | 15 000 | Конус пламени, поджог, тег `#Flamethrower` |

- Оружие — ванильные BOW/CROSSBOW с кастомной моделью и PDC-маркером.
- «Пули» — Arrow без гравитации с маркером; попадание обрабатывается в `EntityDamageByEntityEvent`.
- Разрушения RPG/огнемёта логируются в FlorestRollback для отката.

### Ресурспак
- Java-игрокам пак выдаётся через `Player#setResourcePack` при входе.
- Bedrock-игрокам (Geyser) пак **не выдаётся** — для них используйте `.mcpack` в `plugins/Geyser-Spigot/packs/`.
- При отсутствии Geyser API плагин считает всех игроков Java-игроками (fallback).

### Экономика
- Интеграция с **Vault** (`EconomyManager`).
- Списание средств за запуск дронов и покупку оружия.
- Если Vault не найден — экономика работает в режиме «всё разрешено» (fail-open).

### Обратимые разрушения
- Все массовые разрушения (`ExplosionUtil.makeCrater`, `Flamethrower`) логируются в **FlorestRollback** с тегами.
- Теги позволяют выборочно откатывать последствия конкретного оружия/дрона.
- Без FlorestRollback плагин **отключается** (жёсткая зависимость).

---

## Требования

| Зависимость | Тип | Версия |
|---|---|---|
| **Paper/Purpur** | платформа | 1.21+ |
| **FlorestRollback** | `depend` | 1.5+ |
| **Vault** | `depend` | любая |
| **Geyser-Spigot** | `softdepend` | опционально |
| **Java** | — | 17+ |

**Folia:** поддерживается (`folia-supported: true`). Используются `RegionScheduler`, `AsyncScheduler`, `teleportAsync`.

---

## Установка

1. Установите **FlorestRollback** и **Vault** (обязательно).
2. Опционально установите **Geyser-Spigot** для Bedrock-игроков.
3. Поместите `MillitaryTheme.jar` в `plugins/`.
4. Запустите сервер — плагин создаст конфигурацию и подключится к FlorestRollback.
5. Для Bedrock-игроков положите `.mcpack` в `plugins/Geyser-Spigot/packs/`.

---

## Команды

| Команда | Право | Описание |
|---|---|---|
| `/drone shahed` | `millitarytheme.drone` | Запуск Shahed (20 000) |
| `/drone fpv` | `millitarytheme.drone` | Запуск FPV (10 000) |
| `/drone predator` | `millitarytheme.drone` | Запуск MQ-1 Predator (45 000) |
| `/gun <оружие>` | `millitarytheme.gun` | Покупка оружия |

**Права по умолчанию:** `true` для обоих узлов.

---

## Сборка

### Требования
- **JDK 17+**
- **Gradle 8+** (или используйте wrapper)

### Шаги

```bash
git clone https://github.com/NullPointer-byte/MillitaryTheme.git
cd MillitaryTheme
./gradlew build
```

Готовый артефакт: `build/libs/MillitaryTheme-<version>.jar`.

### Локальная отладка

```bash
./gradlew runServer   # если настроен run-paper / run-task
```

Или скопируйте JAR в `plugins/` тестового сервера с FlorestRollback и Vault.

---

---

## Архитектура

```
ru.millitarytheme
├── MillitaryTheme          # точка входа (JavaPlugin)
├── EconomyManager          # обёртка над Vault
├── ExplosionUtil           # воронки + логирование в FlorestRollback
├── InstallRPOnConnect      # выдача ресурспака (Java/Bedrock)
├── command/
│   ├── DroneCommand        # /drone
│   └── GunCommand          # /gun
├── drone/
│   ├── DroneManager        # реестр активных дронов
│   ├── Drone               # Shahed
│   ├── FpvDrone            # FPV
│   ├── MQ1PredatorDrone    # Predator
│   └── PredatorBomb        # бомба Predator'а
├── guns/
│   ├── Pistol, AK, Shotgun, Sniper, RPG, Flamethrower
└── listener/
    ├── DroneListener       # смерть носителя, выход игрока
    └── PredatorListener    # ЛКМ для сброса бомбы
```

**Ключевые принципы:**
- Оружие — PDC-маркеры на ванильных предметах.
- Дроны — управление через `setVelocity` каждый тик в региональном планировщике.
- Разрушения — снимок `BlockData` **до** поломки → `addLog` в FlorestRollback.
- Folia — все обращения к сущностям/блокам в региональных задачах.

---

## Интеграция с FlorestRollback

Плагин использует **публичный API** FlorestRollback:

```java
plugin.getRollback().addLog(
    tag,              // "#Drone", "#FpvDrone", "#RPG", "#Flamethrower"
    world.getName(),
    x, y, z,
    oldBlockData,     // снимок ДО поломки
    "minecraft:air"   // или "minecraft:fire"
);
```

**Почему ручной `addLog`?** FlorestRollback автологирует `BlockBreakEvent`/`EntityExplodeEvent`, но MillitaryTheme ломает блоки через `breakNaturally()` и `setType()`, которые **не порождают эти события**. Ручной `addLog` — вынужденный и корректный обход.

**Теги** позволяют администратору откатывать последствия конкретного оружия (если FlorestRollback поддерживает фильтр по тегу).

---

## Известные ограничения

- **MQ-1 Predator** не регистрируется в `DroneManager` — инвариант «один дрон на игрока» для него не работает.
- **`config.yml`** пока не реализован — параметры захардкожены.
- **`InstallRPOnConnect`** содержит заглушку URL — до подстановки реального адреса ресурспак Java-игрокам не выдаётся.
- **Bedrock-игроки** не получают пак через плагин — нужен `.mcpack` в Geyser.
---

## Лицензия

MIT — см. `LICENSE`.

---

## Авторы

- **Kirill Dorohov** (NullPointer-byte)
- **Kirill Borisenko** (florestdev)

Связанные проекты: [FlorestRollback](https://github.com/FlorestMCServer/FlorestRollback).
6. **Архитектура** — дерево пакетов, чтобы новый контрибьютор быстро ориентировался.

Если нужно — могу адаптировать под конкретный шаблон (например, с бейджами CI, ссылками на Modrinth/SpigotMC, разделом «Contributing»).
