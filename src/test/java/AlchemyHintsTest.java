package com.example;

import com.codeborne.selenide.Condition;
import org.openqa.selenium.By;
import org.openqa.selenium.Point;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.*;
import java.net.MalformedURLException;
import java.time.Duration;
import java.util.List;
import java.util.regex.Pattern;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.sleep;

public class AlchemyHintsTest extends BaseTest {
    private static final String ALCHEMY_APK = APK_DIR + "/alchemy.apk";
    private static final String ALCHEMY_PACKAGE = "com.ilyin.alchemy";

    private static final By PLAY_BTN = By.xpath("//android.widget.TextView[@text='Play']");
    private static final By HINT_COUNTER = By.xpath("(//android.view.View[@clickable='true'])[1]");
    private static final By WATCH_AD_BTN = By.xpath("//android.widget.TextView[@text='Watch' or @text='Смотреть']");
    private static final By REWARD_MESSAGE = By.xpath(
            "//android.widget.TextView[contains(translate(@text, 'REWARD', 'reward'), 'reward') " +
                    "or contains(translate(@text, 'НАГРАД', 'наград'), 'наград') " +
                    "or contains(@text, 'You got') " +
                    "or contains(@text, 'collected')]");
    private static final By CLOSE_REWARD_BTN = By.xpath("//android.widget.TextView[@text='OK' or @text='Close' or @text='Закрыть']");
    private static final By HINTS_COUNT_TEXT = By.xpath("//android.widget.TextView[contains(@text,'Hints') or contains(@text,'hints')]");

    @BeforeMethod
    public void setUp() throws MalformedURLException {
        startDriver(ALCHEMY_PACKAGE, "com.ilyin.app_google_core.GoogleAppActivity", ALCHEMY_APK);
        sleep(5000);
    }

    @Test(description = "После двух просмотров рекламы количество подсказок должно стать 4")
    public void verifyHintsBecomeFourAfterTwoAds() {
        $(PLAY_BTN).shouldBe(Condition.visible, Duration.ofSeconds(20)).click();
        sleep(3000);

        for (int i = 0; i < 2; i++) {
            System.out.println("Цикл " + (i + 1) + ": открываем окно подсказок");
            $(HINT_COUNTER).shouldBe(Condition.visible, Duration.ofSeconds(15)).click();
            sleep(2000);

// Перед нажатием Watch убедимся, что окно подсказок действительно открыто и кнопка видна
            $(WATCH_AD_BTN).shouldBe(Condition.visible, Duration.ofSeconds(20)).click();
            System.out.println("Цикл " + (i + 1) + ": нажали Watch");

            boolean rewardAppeared = waitForAdCompletion();

            if (rewardAppeared) {
                System.out.println("Цикл " + (i + 1) + ": появилось окно награды, закрываем его");
                $(CLOSE_REWARD_BTN).shouldBe(Condition.visible, Duration.ofSeconds(10)).click();
                sleep(2000);
            } else {
                System.out.println("Цикл " + (i + 1) + ": реклама завершилась, окно награды не появилось");
            }

// Убеждаемся, что вернулись в игровой экран
            $(HINT_COUNTER).shouldBe(Condition.visible, Duration.ofSeconds(30));
// Дополнительная проверка: ищем возможные остаточные крестики и закрываем их
            safeCloseAnyRemainingDialogs();
            sleep(2000);
        }

// Финальная проверка количества подсказок
        $(HINT_COUNTER).shouldBe(Condition.visible, Duration.ofSeconds(10)).click();
        sleep(2000);
        String hintsText = $(HINTS_COUNT_TEXT).shouldBe(Condition.visible).getText();
        int hintsCount = extractNumber(hintsText);
        Assert.assertEquals(hintsCount, 4, "Количество подсказок должно быть 4 после двух просмотров");
    }

    /**
     * Универсальное ожидание завершения рекламы.
     * Обрабатывает цепочку из нескольких реклам, появление крестиков, CAT и окна награды.
     * @return true, если появилось окно награды; false, если просто вернулись в игру.
     */
    private boolean waitForAdCompletion() {
        long startTime = System.currentTimeMillis();
        long timeout = 300000; // 5 минут

        while (System.currentTimeMillis() - startTime < timeout) {
            try {
// Проверяем, не вернулись ли в игру (появление HINT_COUNTER)
                if ($(HINT_COUNTER).is(Condition.visible)) {
                    System.out.println("waitForAdCompletion: HINT_COUNTER виден – возможно, реклама завершилась");
// Дадим небольшую паузу, чтобы убедиться, что это не временное явление
                    sleep(1000);
                    if (!$(HINT_COUNTER).is(Condition.visible)) continue;
                    System.out.println("waitForAdCompletion: HINT_COUNTER стабилен – возвращаем false");
                    return false;
                }

// Проверяем окно награды
                if ($(REWARD_MESSAGE).is(Condition.visible)) {
                    System.out.println("waitForAdCompletion: REWARD_MESSAGE видно – возвращаем true");
                    return true;
                }

// Поиск крестика (ImageView в правой верхней части)
                if (clickCloseButtonIfVisible()) {
                    System.out.println("waitForAdCompletion: нажат крестик (ImageView)");
                    sleep(1000);
                    continue;
                }

// Поиск любых кликабельных элементов справа (CAT, кнопки установки)
                if (clickAnyRightSideButton()) {
                    System.out.println("waitForAdCompletion: нажата кнопка в правой части");
                    sleep(1000);
                    continue;
                }

// Если долго нет действий (более 10 секунд), пробуем координатный клик
                if (System.currentTimeMillis() - startTime > 30000 &&
                        System.currentTimeMillis() - startTime % 10000 < 1000) {
                    System.out.println("waitForAdCompletion: пробуем клик по координатам (1000,200) – возможный крестик");
                    try {
                        Runtime.getRuntime().exec("adb shell input tap 1000 200");
                        sleep(1000);
                        continue;
                    } catch (Exception e) {
                        System.out.println("Ошибка tap: " + e.getMessage());
                    }
                }

                sleep(1000);
            } catch (WebDriverException e) {
                throw new AssertionError("Приложение закрылось (crash) во время просмотра рекламы", e);
            }
        }
        Assert.fail("Реклама не завершилась за " + timeout + " мс (возможно, приложение зависло или реклама зациклилась)");
        return false;
    }

    /** Поиск и клик по крестику (ImageView в правой верхней части) */
    private boolean clickCloseButtonIfVisible() {
        try {
            List<WebElement> elements = driver.findElements(By.xpath("//android.widget.ImageView[@clickable='true']"));
            int screenWidth = driver.manage().window().getSize().getWidth();
            for (WebElement el : elements) {
                if (el.isDisplayed()) {
                    Point loc = el.getLocation();
                    Dimension size = el.getSize();
                    int centerX = loc.getX() + size.getWidth() / 2;
                    int centerY = loc.getY() + size.getHeight() / 2;
// Крестик: правая треть, верхняя половина, небольшой размер
                    if (centerX > screenWidth * 2 / 3 && centerY < screenHeight() / 2 &&
                            size.getWidth() < 150 && size.getHeight() < 150) {
                        el.click();
                        return true;
                    }
                }
            }
        } catch (Exception e) {
// ignore
        }
        return false;
    }

    /** Поиск кликабельной кнопки в правой половине (CAT, кнопки установки) */
    private boolean clickAnyRightSideButton() {
        try {
            List<WebElement> elements = driver.findElements(By.xpath(
                    "//*[@clickable='true' and (" +
                            "local-name()='android.widget.ImageView' or " +
                            "local-name()='android.widget.Button' or " +
                            "local-name()='android.view.View' or " +
                            "local-name()='android.widget.TextView' or " +
                            "local-name()='android.view.ViewGroup')]"));
            int screenWidth = driver.manage().window().getSize().getWidth();
            for (WebElement el : elements) {
                if (el.isDisplayed()) {
                    Point loc = el.getLocation();
                    Dimension size = el.getSize();
                    int centerX = loc.getX() + size.getWidth() / 2;
                    int centerY = loc.getY() + size.getHeight() / 2;
// Элемент в правой половине, не слишком маленький, не системный
                    if (centerX > screenWidth / 2 && size.getWidth() > 30 && size.getHeight() > 30) {
                        String pkg = el.getAttribute("package");
                        if (pkg != null && !pkg.startsWith("android")) {
                            el.click();
                            System.out.println("clickAnyRightSideButton: нажат элемент с координатами " + centerX + "," + centerY);
                            return true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("clickAnyRightSideButton ошибка: " + e.getMessage());
        }
        return false;
    }

    /** Закрыть возможные остаточные диалоги после рекламы (на всякий случай) */
    private void safeCloseAnyRemainingDialogs() {
        try {
            List<WebElement> closeButtons = driver.findElements(By.xpath(
                    "//*[@clickable='true' and (" +
                            "contains(translate(@text, 'ЗАКРЫТЬ', 'закрыть'), 'закрыть') or " +
                            "contains(translate(@text, 'CLOSE', 'close'), 'close') or " +
                            "contains(@text, 'X') or " +
                            "contains(@text, '✕') or " +
                            "contains(translate(@content-desc, 'ЗАКРЫТЬ', 'закрыть'), 'закрыть') or " +
                            "contains(translate(@content-desc, 'CLOSE', 'close'), 'close'))]"));
            for (WebElement btn : closeButtons) {
                if (btn.isDisplayed()) {
                    btn.click();
                    System.out.println("safeClose: закрыт дополнительный диалог");
                    sleep(1000);
                }
            }
        } catch (Exception e) {
// ignore
        }
    }

    private int screenHeight() {
        return driver.manage().window().getSize().getHeight();
    }

    private int extractNumber(String text) {
        java.util.regex.Matcher matcher = Pattern.compile("\\d+").matcher(text);
        return matcher.find() ? Integer.parseInt(matcher.group()) : 0;
    }
}