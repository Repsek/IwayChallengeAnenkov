import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.*;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ChitaiGorodTests {
    private WebDriver driver;
    private WebDriverWait wait;

    @BeforeAll
    public static void webDriverSetup() {
        WebDriverManager.chromedriver().setup();
    }

    @BeforeEach
    public void setUp() {
        driver = new ChromeDriver();
        wait = new WebDriverWait(driver, 5);
        driver.manage().timeouts().implicitlyWait(5, TimeUnit.SECONDS);
    }

    @AfterEach
    public void tearDown() {driver.quit();}


    @RepeatedTest(10)
    public void mainPage_booksSearchedThenAddedToBasketThenOneDeleted_booksAddedAndRemovedProperly() throws InterruptedException {
        String mainPageAddress = "https://www.chitai-gorod.ru/";
        int booksToTest = 3;
        By searchFieldLocator = By.className("search-form__input");
        By searchButtonLocator = By.className("search-form__btn");
        By bookTitleLocator = By.className("product-card__title");
        By bookAuthorLocator = By.className("product-card__author");
        By bookPriceLocator = By.className("price");
        By buyButtonLocator = By.className("product-card__button");
        By basketLocator = By.className("basket__logo");
        By basketTitleLocator = By.className("basket-item__name");
        By basketAuthorLocator = By.className("basket-item__author");
        By basketPriceLocator = By.className("basket-item__price-total_discount");
        By basketTotalPrice = By.className("js__total_sum");
        By deleteButtonLocator = By.className("basket-item__control_delete");


        driver.manage().window().maximize();
        driver.navigate().to(mainPageAddress);
        driver.findElement(searchFieldLocator).sendKeys("тестирование");
        driver.findElement(searchButtonLocator).click();

//        Чтобы не создавать множество массивов, сделал отдельный метод, который из списка вебэлементов берёт текст и сохраняет в массив строк
        var allBuyButtons = driver.findElements(buyButtonLocator);
        List<String> bookTitles = getLocatorsTexts(bookTitleLocator);
        List<String> authorNames = getLocatorsTexts(bookAuthorLocator);

//        В дальнейшем нам придётся сравнивать суммы, а все цены на странице результатов поиска идут со знаком ₽, поэтому отдельный метод, записывающий цены в числовой список.
        List<Integer> bookPrices = getLocatorsNumbers(bookPriceLocator);

//        В результатах поиска вместо кнопки "Купить" у некоторых книг бывает кнопка "Где купить?", при нажатии которой происходит переход на страницу с картой. Цикл ниже удаляет из массивов элементы, у которых кнопка не "Купить"
        for (int i = 0; i < allBuyButtons.size(); i++) {
            var currentElement = allBuyButtons.get(i).getAttribute("data-status");
            if (!currentElement.equals("buy")) {
                allBuyButtons.remove(i);
                bookTitles.remove(i);
                authorNames.remove(i);
                bookPrices.remove(i);
            }
        }

//        Для добавления трёх случайных книг в корзину (в задании написано "любые", что можно трактовать как случайные, и для тестов это гораздо лучше) написан отдельный метод, создающий массив случайных чисел нужной длины
        var numbersToTest = getRandomNumbers(bookTitles, booksToTest);
        List<String> titlesToTest = new ArrayList<>();
        List<String> authorsToTest = new ArrayList<>();
        List<Integer> pricesToTest = new ArrayList<>();

//        Цикл записывает название, авторов и цены выбранных книг, а затем кликает на кнопку "Купить". Sleep здесь добавлен из-за того, что без него все три кнопки не нажимались никогда. Добавление частично устранило проблему, но иногда тест всё же валится.
        for (int i = 0; i < booksToTest; i++) {
            var currentNumber = numbersToTest.get(i);
            titlesToTest.add(bookTitles.get(currentNumber));
            authorsToTest.add(authorNames.get(currentNumber));
            pricesToTest.add(bookPrices.get(currentNumber));
            allBuyButtons.get(currentNumber).click();
            Thread.sleep(1000);
        }

        driver.findElement(basketLocator).click();
        List<String> basketTitles = getLocatorsTexts(basketTitleLocator);
        List<String> basketAuthors = getLocatorsTexts(basketAuthorLocator);
        List<Integer> basketPrices = getLocatorsNumbers(basketPriceLocator);
        wait.until(ExpectedConditions.presenceOfElementLocated(basketTotalPrice));
        int actualSum = Integer.parseInt(driver.findElement(basketTotalPrice).getText());
        int expectedSum = sum(basketPrices);

        Assertions.assertEquals(titlesToTest, basketTitles, "Названия выбранных книг не совпадают с названиями книг в корзине");
        Assertions.assertEquals(authorsToTest, basketAuthors, "Авторы выбранных книг не совпадают с авторами книг в корзине");
        Assertions.assertEquals(pricesToTest, basketPrices, "Цены выбранных книг не совпадают с ценами книг в корзине");
        Assertions.assertEquals(expectedSum, actualSum, "Общая стоимость заказа не совпадает с суммой цен выбранных книг");

        var deleteButtons = driver.findElements(deleteButtonLocator);

//        Какую книгу удалять из корзины также выбирается случайным образом
        var randomNumber = (int) (Math.random() * deleteButtons.size());
        deleteButtons.get(randomNumber).click();
        expectedSum = expectedSum - basketPrices.get(randomNumber);

//        Без sleep сумма заказа не успевает измениться и тест всегда валится из-за не соответствия ожидаемой и фактической суммы заказа
        Thread.sleep(2000);
        actualSum = Integer.parseInt(driver.findElement(basketTotalPrice).getText());

        Assertions.assertEquals(expectedSum, actualSum, "Общая стоимость заказа после удаления одной книги не совпадает с ожидаемой суммой");
    }

//    Метод для записи текста вебэлементов в строковый массив по локатор
    public List<String> getLocatorsTexts (By locator) {
        var webElements = driver.findElements(locator);
        var strings = new ArrayList<String>();

        for (int i = 0; i < webElements.size(); i++) {
            String locatorText = webElements.get(i).getText();
            if (!locatorText.equals("")) {
                strings.add(locatorText);
            }
        }
        return strings;
    }

//    Метод для записи текста вебэлементов в строковый массив по локатор
    public List<Integer> getLocatorsNumbers (By locator) {
        var webElements = driver.findElements(locator);
        var prices = new ArrayList<Integer>();

        for (int i = 0; i < webElements.size(); i++) {
            String locatorText = webElements.get(i).getText();
            if (!locatorText.equals("")) {
                locatorText = locatorText.substring(0, (locatorText.length()-2));
                prices.add(Integer.parseInt(locatorText));
            }
        }
        return prices;
    }

//    Метод для получения нужного числа случайных чисел из диапазона указанного списка
    public List<Integer> getRandomNumbers (List<String> titles, int quantity) {
        List<Integer> randomNumbers = new ArrayList<>();
        while (randomNumbers.size() < quantity) {
            var number = (int) (Math.random() * titles.size());
            if (!randomNumbers.contains(number)) {
                randomNumbers.add(number);
            }
        }
        Collections.sort(randomNumbers);
        return randomNumbers;
    }

//    Сумма всех членов числового списка
    public int sum(List<Integer> list) {
        int sum = 0;
        for (int i = 0; i < list.size(); i++) {
            sum = sum + list.get(i);
        }
        return sum;
    }
}
