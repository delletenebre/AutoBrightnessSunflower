# Подсолнух
Приложение для Android позволяющее автоматически устанавливать яркость экрана по позиции солнца.

Проект разработан для планшета установленного в качестве CarPC.

![screenshot_20151219-211925](https://cloud.githubusercontent.com/assets/3936845/11914353/c62863b8-a6a8-11e5-9d57-580bd767cb45.png)
![screenshot_20151219-211947](https://cloud.githubusercontent.com/assets/3936845/11914351/c625f218-a6a8-11e5-81ec-06d45f6a4071.png)
![screenshot_20151219-212004](https://cloud.githubusercontent.com/assets/3936845/11914355/c6294e18-a6a8-11e5-9934-dace4204a18b.png)
![screenshot_20151219-212016](https://cloud.githubusercontent.com/assets/3936845/11914354/c628e522-a6a8-11e5-824d-ea55b6c7f12a.png)

### Требования
* Android 4.0.3 и выше
* Интернет

### Алгоритм работы
При разблокировке экрана (`ACTION_USER_PRESENT`) проверяется текущее время и устанавливается один из трёх режимов: день, сумерки, ночь. Для каждого из режимов в настройках можно установить желаемый уровень яркости экрана.

При включенной опции **`Изменять яркость при включенном экране`** запускается фоновый сервис, который через заданный интервал (**`Частота проверки актуальности режима`**) будет проверять время и актуальность яркости экрана.

При блокировке экрана (`ACTION_SCREEN_OFF`) фоновый сервис завершит работу.

### Android 6.x
Начиная с версии Android Marshmallow, при запуске будет запущено окно настроек `Settings.ACTION_MANAGE_WRITE_SETTINGS`, где необходимо разрешить программе изменение системных настроек. Программа изменяет только один пункт - отключает автоматическую (адаптивную) настройку яркости экрана.

![screenshot_20151219-232103](https://cloud.githubusercontent.com/assets/3936845/11914352/c625fe70-a6a8-11e5-831a-4614c03f077d.png)

### ...
* Геокодер: [OpenStreetMap Nominatim](https://nominatim.openstreetmap.org/)
* Расписание восхода/заката Солнца: [Sunrise-Sunset](http://sunrise-sunset.org/)
