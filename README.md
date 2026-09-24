# iPhone Pushes

Android-приложение, которое читает уведомления MAX, VK и других выбранных приложений и отправляет их на iPhone в [Bark](https://github.com/Finb/Bark).

Не хранит переписку на своём сервере. Ключ Bark остаётся на телефоне. Пуш уходит на `api.day.app`.

## Скачать

Актуальная версия: **1.3**

Готовый APK: [Releases](https://github.com/teazy865/bark-forwarder/releases)

Если релиза 1.3 ещё нет в Releases, скачайте APK из последнего успешного [Actions](https://github.com/teazy865/bark-forwarder/actions) → Build APK → Artifacts → `bark-forwarder-apk`.

Установка с Android: разрешите установку из этого источника и откройте APK.

С **1.1** нужно один раз удалить приложение и поставить 1.3 заново (другая подпись). С **1.3** и дальше можно обновлять поверх.

## Что нового в 1.3

- Тёмный интерфейс, карточки приложений, статус ключа / доступа / службы
- По тапу на iPhone открывается нужное приложение
- Для VK по умолчанию `vk://vk.com/im` (сообщения, не сайт)
- Для MAX по умолчанию `https://web.max.ru`
- Свои иконки в пушах Bark
- Лента последних пересланных уведомлений
- Постоянная подпись APK — следующие версии ставятся как обновление

## Как пользоваться

1. Установите Bark на iPhone и скопируйте ключ (только набор символов, без `https://api.day.app/`).
2. Откройте **iPhone Pushes**, вставьте ключ.
3. MAX и VK уже в списке. Другое приложение — через «Добавить приложение».
4. Нажмите «Доступ» и разрешите **iPhone Pushes**.
5. В шторке должно висеть **Running. Do not dismiss.** Его нельзя смахивать.
6. «Тестовый пуш» проверяет ключ.
7. Нажмите карточку MAX или VK, чтобы сменить URL открытия и иконку.

Рекомендуемые URL:

- VK: `vk://vk.com/im`
- MAX: `https://web.max.ru` или `max://`

Иконки по умолчанию:

- VK: `https://raw.githubusercontent.com/teazy865/icons/refs/heads/main/IMG_0251.png`
- MAX: `https://raw.githubusercontent.com/teazy865/icons/refs/heads/main/IMG_0246.png`

## Важно

- Телефон с приложением должен быть включён и в сети.
- На Xiaomi / Redmi / POCO: автозапуск и батарея без ограничений.
- Официальные пакеты: MAX `ru.oneme.app`, VK `com.vkontakte.android`, VK Messenger `com.vk.im`.
- Это не официальный клиент MAX/VK. Ответить в чат из баннера Bark нельзя.
- Пустой `vk://` часто открывает сайт внутри VK. Используйте `vk://vk.com/im`.

## Сборка

GitHub → Actions → Build APK → Run workflow.

## Автор

teazy865
