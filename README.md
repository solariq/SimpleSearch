## Лёгкий поиск

У нас есть [GDrive][1] где можно найти архивы со статьями из Википедии,
 а так же **можно** добавлять свои файлы и библиотеки.
 
`Mini_Wiki.zip` - Архив с 10_000 статей, для быстрой проверки корректности.

`wikiforia_dump_splitted.zip` - Полный архив. (Тестится минимум 4 часа)


### Запуск

Перед запуском приложения необходимо в классе `SearchServer.java` изменить значение переменной `pathToZIP` на путь до архива с Википедией. 

Например: 
```
pathToZIP = Paths.get("../WikiDocs/wikiforia_dump_splitted.zip");
```

### Ресурсы (папка resources)

Необходимо добавить в папку resources файл [mystem][2].



[1]: https://drive.google.com/drive/folders/1JGMrne_8oFg5V6bvbEb88nTbRJ830u1C?usp=sharing
[2]: https://tech.yandex.ru/mystem/