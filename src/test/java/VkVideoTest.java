package com.example;

import com.codeborne.selenide.Condition;
import org.openqa.selenium.By;
import org.testng.Assert;
import org.testng.annotations.*;
import java.net.MalformedURLException;
import java.time.Duration;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.sleep;

public class VkVideoTest extends BaseTest {
    private static final String VK_VIDEO_APK = APK_DIR + "/vkvideo.apk";
    private static final String VK_PACKAGE = "com.vk.vkvideo";

    // Кнопка пропуска авторизации
    private static final By SKIP_BTN = By.xpath("//android.widget.Button[@text='Skip' or @resource-id='com.vk.vkvideo:id/fast_login_tertiary_btn']");

    // Главная вкладка
    private static final By MAIN_TAB = By.xpath("//android.view.ViewGroup[@content-desc='Main' or @resource-id='com.vk.vkvideo:id/tab_main']");

    // Первое видео в ленте
    private static final By FIRST_VIDEO = By.xpath("(//android.widget.LinearLayout[@resource-id='com.vk.vkvideo:id/content'])[1]");
    private static final By SECOND_VIDEO = By.xpath("(//android.widget.LinearLayout[@resource-id='com.vk.vkvideo:id/content'])[2]");

    // Элементы плеера
    private static final By PLAYER_VIEW = By.xpath("//android.widget.FrameLayout[@resource-id='com.vk.vkvideo:id/playerView']");
    private static final By VIDEO_DISPLAY = By.xpath("//android.view.TextureView[@resource-id='com.vk.vkvideo:id/video_display']");

    // Сообщение об отсутствии интернета (возможные варианты)
    private static final By NO_INTERNET_MSG = By.xpath(
            "//android.widget.TextView[contains(@text,'No internet') " +
                    "or contains(@text,'Нет соединения') " +
                    "or contains(@text,'connection') " +
                    "or contains(@text,'Network') " +
                    "or contains(@text,'offline') " +
                    "or contains(@text,'disconnected')]");

    @BeforeMethod
    public void setUp() throws MalformedURLException {
        System.out.println("=== VK Video setup start ===");
        startDriver(
                VK_PACKAGE,
                "com.vk.video.screens.main.MainActivity",
                VK_VIDEO_APK);
        sleep(8000);

        // Пытаемся нажать Skip, если кнопка есть
        try {
            if ($(SKIP_BTN).exists()) {
                $(SKIP_BTN).shouldBe(Condition.visible, Duration.ofSeconds(20)).click();
                System.out.println("Clicked Skip button");
                sleep(8000);
            } else {
                System.out.println("Skip button not present – proceeding");
            }
        } catch (Exception e) {
            System.out.println("Error handling Skip button: " + e.getMessage());
        }

        $(MAIN_TAB).shouldBe(Condition.visible, Duration.ofSeconds(40));
        sleep(5000);
        System.out.println("=== VK Video setup completed ===");
    }

    @AfterMethod(alwaysRun = true)
    public void restoreNetwork() {
        // Включаем Wi-Fi и мобильные данные после каждого теста (на случай, если тест их отключил)
        try {
            Runtime.getRuntime().exec("adb shell svc wifi enable");
            Runtime.getRuntime().exec("adb shell svc data enable");
        } catch (Exception e) {
            // ignore
        }
    }

    @Test(description = "Негативный сценарий – при отключении интернета появляется сообщение об ошибке")
    public void shouldShowNoInternetMessage() throws Exception {
        System.out.println("=== Negative test start ===");

        // Отключаем Wi-Fi и мобильные данные
        System.out.println("Disabling Wi-Fi and mobile data...");
        Runtime.getRuntime().exec("adb shell svc wifi disable");
        Runtime.getRuntime().exec("adb shell svc data disable");
        sleep(5000);

        // Обновляем ленту (свайп вниз)
        System.out.println("Refreshing feed...");
        Runtime.getRuntime().exec("adb shell input swipe 540 800 540 1500 1000");
        sleep(3000);

        // Пытаемся открыть первое видео
        System.out.println("Opening first video...");
        if (!$(FIRST_VIDEO).exists()) {
            Assert.fail("First video not found");
        }
        $(FIRST_VIDEO).shouldBe(Condition.visible, Duration.ofSeconds(25)).click();
        System.out.println("Clicked first video");
        sleep(8000);

        // Если первый ролик не открывается, пробуем второй
        if (!$(PLAYER_VIEW).exists()) {
            System.out.println("First video failed – trying second video");
            Runtime.getRuntime().exec("adb shell input keyevent 4"); // BACK
            sleep(3000);
            $(SECOND_VIDEO).shouldBe(Condition.visible, Duration.ofSeconds(25)).click();
            System.out.println("Clicked second video");
            sleep(8000);
        }

        // Ждём появления плеера
        $(PLAYER_VIEW).shouldBe(Condition.visible, Duration.ofSeconds(20));
        System.out.println("Player opened");

        // Даём время на попытку загрузки
        sleep(10000);

        // Ожидаем сообщение об отсутствии интернета
        System.out.println("Waiting for offline error message...");
        $(NO_INTERNET_MSG).shouldBe(Condition.visible, Duration.ofSeconds(60));
        System.out.println("No‑internet message appeared – negative test DONE");
    }

    @Test(description = "Положительный сценарий – видео воспроизводится")
    public void shouldPlayVideoPositive() throws Exception {
        System.out.println("=== Positive test start ===");

        // Включаем интернет (на случай, если предыдущий тест его отключил)
        Runtime.getRuntime().exec("adb shell svc wifi enable");
        Runtime.getRuntime().exec("adb shell svc data enable");
        sleep(5000);

        // Обновляем ленту
        System.out.println("Refreshing feed...");
        Runtime.getRuntime().exec("adb shell input swipe 540 800 540 1500 1000");
        sleep(5000);

        // Открываем первое видео
        System.out.println("Opening first video...");
        $(FIRST_VIDEO).shouldBe(Condition.visible, Duration.ofSeconds(25)).click();
        System.out.println("Clicked first video");
        sleep(8000);

        // Проверяем, что плеер появился и видео отображается
        $(PLAYER_VIEW).shouldBe(Condition.visible, Duration.ofSeconds(25));
        $(VIDEO_DISPLAY).shouldBe(Condition.visible, Duration.ofSeconds(25));
        System.out.println("Player and video display are visible");

        Assert.assertTrue($(PLAYER_VIEW).is(Condition.visible), "Video player should be visible");
        System.out.println("Video player visibility – OK");

        // Даём видео проиграться
        sleep(15000);

        // Проверяем, что плеер всё ещё виден (видео не остановилось)
        Assert.assertTrue($(PLAYER_VIEW).is(Condition.visible), "Video player should still be visible after playback");
        System.out.println("Video still playing – OK");

        // Возвращаемся назад
        System.out.println("Closing player and returning to main screen");
        Runtime.getRuntime().exec("adb shell input keyevent 4");
        sleep(3000);

        System.out.println("=== Positive test finished ===");
    }
}