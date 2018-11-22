## Лёгкий поиск

[![Build Status](https://travis-ci.org/solariq/sensearch.svg?branch=master)](https://travis-ci.org/solariq/sensearch)

У нас есть [GDrive][1] где можно найти архивы со статьями из Википедии,
 а так же **можно** добавлять свои файлы и библиотеки.
 
`Mini_Wiki.zip` - Архив с небольшим количеством статей, для тестов и экспериментов.

`Mini_Wiki_categories.zip` - Архив с 60k+ статьями из следующих категорий: 

     "Актёры России"
     "Футболисты России"
     "Писатели России по алфавиту"
     "Поэты России"
     "Актрисы России"
     "Мастера спорта России международного класса"
     "Правители"
     и что-то про города
      
Кажется, что в дампе википедии есть некоторый баг, из-за которого не у всех статьей проставлены все категории (например, у статьи про Россию в дампе только две категории), поэтому есть некоторая вероятность того, что в этом архиве для каждой категории есть не все статьи. 

`wikiforia_dump_splitted.zip` - Полный архив. (без категорий)


### Запуск

Для того чтобы запустить фронтенд, необходимо сделать следующее:
```
sudo apt-get install npm
sudo apt-get install nodejs
```

Затем нужно проверить что поставилась `node` нужной (8+) версии:
```
node -v
```
Если вам не повезло и поставилась старая версия (например, на Ubuntu 16.04 ставится 4 версия), 
то качаем [nodejs][3].

Дальше нужно сделать:
```
sudo npm install -g @angular/cli
cd webapp
npm install
npm install --save @angular/material @angular/cdk @angular/animations
```

Теперь у нас поставлено все необходимое, дальше необходимо все это собрать, запустив это из папки `webapp`:
```
ng build
```
После чего можно запускать сервак и все будет работать.

Если будут выпадать ошибки вида `Cannot find module <module-name>`, 
то необходимо пставить и эти модули
```
npm install --save <module-name>
```
**Повторять до успеха**

**Замечание**: если вдруг что-то поменялось на фронтенде, то для того, чтобы эти изменения вступили в силу, нужно запустить `ng build`.

Перед запуском приложения необходимо в файде `config.json` изменить значение переменной `pathToZIP` на путь до архива с Википедией. 


### Ресурсы (папка resources)

Необходимо добавить в папку resources файл [mystem][2].

Добавьте в папку `resources` архив `vectors.txt.gz`, чтобы иметь возможность
работать с *Word Embeddings*. Если хотите убедиться, что всё сделали правильно, запустите тест `NearestFinderTest`. 

`vectors50.txt.gz` - векторы размерности 50 (~800 Мб вашей оперативки)

**P.s.** При необходимости используйте флаг `-Xmx<size>`, чтобы увеличить размер heap.

### Метрика

В проекте существует метрика, которая в данный момент оценивает ранжировку относительно Google.
При каждом запросе автоматически выводится DCG запроса в консоль, а так же в папку `resources/Metrics/<request_name>` складывается файл со значением DCG.

Так же есть классы `RebaseMetrics` и `MetricTest` для оценки измениений ранжировки относительно статических данных от Google и запросов.

__Инструкция по применению__
>Если вы уже запускали `RebaseMetrics`, то можете сразу перейти к пункту 2.

1. Запустить `RebaseMetrics`, после чего в папке `resources/Metrics/` появятся текущие результаты выдачи Google на популярные запросы в Википедию по нашим категориям. Так же необоходимо не забыть остановить исполнение данного класса.
2. Запустить `MetricTest`, после чего на кансоли появится значения старой метрики и новой по всем запросам из файла `resources/Queries.txt`.
3. Теперь можно оценивать прогресс.

[1]: https://drive.google.com/drive/folders/1JGMrne_8oFg5V6bvbEb88nTbRJ830u1C?usp=sharing
[2]: https://tech.yandex.ru/mystem/
[3]: http://nodejs.org
