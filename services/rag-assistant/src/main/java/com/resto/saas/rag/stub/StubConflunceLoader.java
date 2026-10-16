package com.resto.saas.rag.stub;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * заглушка Конфлюенс Апи
 */
@Slf4j
@Component
public class StubConflunceLoader {

    public record Article (
        String title,
        String content,
        String aggregation,
        String documentType,
        String url
    ) {}

    public List<Article> loadArticles() {
        log.info("Загружаем статьи из Конфлюенс");

        List<Article> articles = new ArrayList<>();
        articles.add(new Article(
                "Подключение Glovo: пошаговый гайд",
                """
                Для подключения Glovo необходимо выполнить следующие шаги:
                
                1. Зарегистрировать ресторан в партнёрском портале Glovo (partners.glovoapp.com)
                2. Получить API-ключи: clientId и clientSecret в разделе "Интеграции"
                3. В Kwaaka админке перейти в Настройки → Интеграции → Glovo
                4. Ввести clientId, clientSecret и webhook URL
                5. Нажать "Проверить соединение" — должен прийти тестовый пинг
                
                Частые ошибки:
                - 401 Unauthorized: неверный clientSecret или истёк токен
                - 403 Forbidden: ресторан не активирован менеджером Glovo
                - Timeout: проверить firewall — Glovo требует исходящие соединения на порт 443
                
                После успешного подключения синхронизация меню запускается автоматически каждые 15 минут.
                Первый заказ обычно приходит в течение 1-2 часов после активации.
                """,
                "GLOVO",
                "ONBOARDING",
                "https://confluence.kwaaka.com/pages/glovo-onboarding"
        ));
        articles.add(new Article(
                "Glovo: не синхронизируется меню",
                """
                Если меню Glovo не синхронизируется, проверьте:
                
                1. Статус интеграции в Kwaaka → Интеграции → Glovo
                   - Если "ERROR" — посмотреть лог ошибок
                   - Если "PENDING" — подождать до 15 минут
                
                2. Логи синхронизации:
                   grep "GlovoMenuSync" /var/log/kwaaka/menu-sync.log
                
                Частые причины сбоев:
                - Ошибка 422: в меню есть товары без категории или цены
                - Ошибка 413: размер меню больше 10MB (разбейте на категории)
                - Ошибка 504: Glovo API недоступен — подождать 5-10 минут
                
                Ручной перезапуск: Kwaaka → Интеграции → Glovo → "Пересинхронизировать сейчас"
                """,
                "GLOVO",
                "TROUBLESHOOTING",
                "https://confluence.kwaaka.com/pages/glovo-menu-sync-issues"
        ));
        // === Wolt ===
        articles.add(new Article(
                "Подключение Wolt: инструкция",
                """
                Wolt использует OAuth 2.0 для авторизации. Процесс подключения:
                
                1. Запросить доступ к Wolt Partner API через менеджера Kwaaka
                2. Получить merchant_id и API credentials
                3. В Kwaaka настроить: merchant_id, client_id, client_secret
                4. Авторизоваться через OAuth flow (автоматически)
                5. Настроить маппинг категорий меню под требования Wolt
                
                Требования Wolt к меню:
                - Все цены в местной валюте
                - Обязательно поле "ingredients" (состав блюда)
                - Минимум одна фотография на категорию
                - Вес блюд в граммах
                
                Синхронизация каждые 30 минут. Webhook от Wolt приходит на указанный URL.
                """,
                "WOLT",
                "ONBOARDING",
                "https://confluence.kwaaka.com/pages/wolt-onboarding"
        ));

        articles.add(new Article(
                "Wolt: заказы не приходят в Kwaaka",
                """
                Если заказы Wolt не поступают в систему, проверьте:
                
                1. Webhook endpoint доступен из интернета:
                   curl -X POST https://api.kwaaka.com/webhooks/wolt -H "Content-Type: application/json" -d '{"test":true}'
                
                2. SSL-сертификат валидный (Wolt не работает с self-signed)
                
                3. Логи webhook:
                   grep "WoltWebhook" /var/log/kwaaka/webhooks.log
                
                4. Проверить подпись HMAC-SHA256 в заголовке X-Wolt-Signature
                
                Если webhook получает заказы, но они не создаются в Kwaaka:
                - Проверить маппинг категорий Wolt → Kwaaka
                - Проверить наличие блюд из заказа в меню Kwaaka
                - Смотреть логи: grep "WoltOrderMapping" /var/log/kwaaka/orders.log
                """,
                "WOLT",
                "TROUBLESHOOTING",
                "https://confluence.kwaaka.com/pages/wolt-orders-missing"
        ));

        // === YandexEda ===
        articles.add(new Article(
                "Яндекс Еда: частые вопросы",
                """
                FAQ по интеграции с Яндекс Едой:
                
                Q: Как часто обновляются цены?
                A: Автоматически каждые 20 минут. Можно вручную через "Синхронизировать сейчас".
                
                Q: Что делать если ресторан временно закрыт?
                A: В Kwaaka → Настройки → Режим работы → "Временно закрыть на агрегаторах".
                   Яндекс Еда автоматически перестанет принимать заказы.
                
                Q: Как отменить заказ после принятия?
                A: Только через службу поддержки Яндекс Еды. Kwaaka может только отметить как "Отменён".
                
                Q: Почему комиссия отличается от договора?
                A: Проверьте раздел "Тарифы" в кабинете Яндекс Еды. Комиссия может меняться по акции.
                
                Q: Как настроить доставку силами ресторана?
                A: В настройках интеграции выбрать "Собственная доставка" вместо "Доставка Яндекса".
                """,
                "YANDEX_EDA",
                "FAQ",
                "https://confluence.kwaaka.com/pages/yandex-eda-faq"
        ));

        articles.add(new Article(
                "Яндекс Еда: ошибка синхронизации меню 400",
                """
                Ошибка 400 при синхронизации с Яндекс Едой означает некорректные данные в меню.
                
                Проверьте:
                1. Все ли блюда имеют ненулевую цену (Яндекс не принимает 0.00)
                2. Длина названия блюда ≤ 100 символов
                3. Описание блюда ≤ 1000 символов
                4. Модификаторы (топпинги) имеют уникальные имена в пределах блюда
                5. Стоп-листы не содержат несуществующих блюд
                
                Лог с деталями ошибки:
                /var/log/kwaaka/yandex-sync-errors.log
                
                Команда для поиска проблемных блюд:
                curl "https://api.kwaaka.com/api/v1/debug/yandex/menu-validation?tenantId=xxx"
                
                После исправлений запустите ручную синхронизацию.
                """,
                "YANDEX_EDA",
                "TROUBLESHOOTING",
                "https://confluence.kwaaka.com/pages/yandex-eda-400-error"
        ));

        // === Общий ===
        articles.add(new Article(
                "Общий регламент: что делать если агрегатор не отвечает",
                """
                Универсальный алгоритм при проблемах с любым агрегатором:
                
                1. Проверить статус API агрегатора:
                   - Glovo: status.glovoapp.com
                   - Wolt: status.wolt.com
                   - Яндекс Еда: status.yandex.ru/eda
                
                2. Если API работает — проверить интеграцию в Kwaaka:
                   Админка → Интеграции → [Агрегатор] → "Проверить соединение"
                
                3. Проверить логи:
                   /var/log/kwaaka/aggregator-[name].log
                
                4. Если проблема на стороне Kwaaka — создать тикет в Jira с тегами:
                   - component: aggregator-integration
                   - priority: high (если заказы не идут)
                   - attach: последние 100 строк лога
                
                5. Временно отключить агрегатор через админку, чтобы не терять заказы.
                
                SLA на решение проблем с агрегаторами: 2 часа в рабочее время.
                """,
                "ALL",
                "TROUBLESHOOTING",
                "https://confluence.kwaaka.com/pages/aggregator-general-troubleshooting"
        ));
        log.info("Загружено {} статей из Confluence", articles.size());
        return articles;
    }
}
