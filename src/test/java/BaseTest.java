package com.example;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.WebDriverRunner;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.remote.AndroidMobileCapabilityType;
import io.appium.java_client.remote.MobileCapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterClass;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;

public class BaseTest {
    protected static final String APK_DIR = "src/test/resources/apk";
    protected AndroidDriver driver;

    protected void startDriver(String appPackage, String appActivity, String appPath) throws MalformedURLException {
        DesiredCapabilities caps = new DesiredCapabilities();

        caps.setCapability(MobileCapabilityType.PLATFORM_NAME, "Android");
        caps.setCapability(MobileCapabilityType.AUTOMATION_NAME, "UiAutomator2");
        caps.setCapability(MobileCapabilityType.DEVICE_NAME, "Android Emulator");
        caps.setCapability(MobileCapabilityType.NEW_COMMAND_TIMEOUT, 300);
        caps.setCapability(MobileCapabilityType.NO_RESET, false);
        caps.setCapability("autoGrantPermissions", true);
        caps.setCapability("adbExecTimeout", 60000);
        caps.setCapability("uiautomator2ServerLaunchTimeout", 90000);
        caps.setCapability("uiautomator2ServerInstallTimeout", 90000);

        caps.setCapability(MobileCapabilityType.APP, appPath);
        caps.setCapability(AndroidMobileCapabilityType.APP_PACKAGE, appPackage);
        caps.setCapability(AndroidMobileCapabilityType.APP_ACTIVITY, appActivity);

        driver = new AndroidDriver(new URL("http://127.0.0.1:4723/"), caps);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(15));

        WebDriverRunner.setWebDriver(driver);
        Configuration.timeout = 30000;
        Configuration.pollingInterval = 500;
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        // Включаем интернет
        try {
            Runtime.getRuntime().exec("adb shell svc wifi enable");
            Runtime.getRuntime().exec("adb shell svc data enable");
        } catch (Exception e) {
            // ignore
        }

        // Принудительно останавливаем приложения
        try {
            Runtime.getRuntime().exec("adb shell am force-stop com.ilyin.alchemy");
            Runtime.getRuntime().exec("adb shell am force-stop com.vk.vkvideo");
            Thread.sleep(1000); // даём время на остановку
        } catch (Exception e) {
            // ignore
        }

        // Закрываем драйвер
        if (driver != null) {
            try {
                driver.quit();
            } catch (Exception e) {
                // ignore
            } finally {
                driver = null;
            }
        }

        // Дополнительная проверка – убиваем процессы ещё раз
        try {
            Runtime.getRuntime().exec("adb shell am force-stop com.ilyin.alchemy");
            Runtime.getRuntime().exec("adb shell am force-stop com.vk.vkvideo");
        } catch (Exception e) {
            // ignore
        }

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            // ignore
        }
    }

    @AfterClass(alwaysRun = true)
    public void quitDriver() {
        if (driver != null) {
            try {
                driver.quit();
            } catch (Exception e) {
                // ignore
            }
        }
    }
}