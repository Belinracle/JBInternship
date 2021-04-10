# JBInternship
IntelliJ: Подключение по SSH через прокси-серверы

# Задача
Предлагается написать клиентское и серверное приложение на Java или на Kotlin. Приложение должно использовать только стандартную библиотеку Java, никакие сторонние фреймворки или библиотеки не должны использоваться. Оба приложения стоит реализовать в одном java/kt-файле и включать клиент или сервер в зависимости от аргументов командной строки.

Клиент:

* Работает либо в терминале (bash/cmd.exe), либо в IDE.
* Принимает через аргументы командной строки хост и порт.
* Соединяется с сервером и использует одно TCP-соединение всё время своей работы.
* Ждёт от пользователя ввод числа.
* Отправляет число на сервер.
* Принимает результат с сервера и выводит результат на экран. Результат всегда помещается в одну строку, в конце которой есть "\n".
* При вводе пустой строки клиент отсоединяется от сервера и завершает работу.(Модифицировано мной: клиент выключается, если пишет STOP)

Сервер:

* Работает либо в терминале (bash/cmd.exe), либо в IDE.
* Принимает через аргументы командной строки хост и порт для прослушивания.
* Должен уметь работать одновременно с двумя и более клиентами.
* От каждого клиента сервер ждёт число N.
* Сервер подсчитывает N-ое число Фибоначчи и отправляет его клиенту.
* После отправки сервер не разрывает соединение с клиентом, а ждёт от клиента следующее число.