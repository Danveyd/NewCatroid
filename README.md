# NewCatroid #

Это наш проект на основе Pocket Code
больше писать нечего, репозиторий закрытый :/

вот краткий гайд разработчикам:

**перед началом работы загрузи свежие изменения:**

```bash
git pull origin main
```


**для изменений:**
```bash
git add .
git commit -m "Описание изменений"
```


**Обновить репозиторий:**
```bash
git push origin main
```


**Ветки, чтобы не ломать основной проект у других:**
```bash
git checkout -b feature/new-feature   # создаём ветку, тут пиши название
git add .
git commit -m "Добавил новую фичу"
git push origin feature/new-feature  # отправляем её в репозиторий
```


**Еще больше работ с ветками:**
Список веток
```bash
git branch
```


**вернуться в основную**
```bash
git checkout main
```


**Отправить новую ветку**
```bash
git push -u origin feature/new-feature
```


**Объединить новую ветку (фичу) с основным проектом:**
```bash
git checkout main
git pull origin main #подтягиваем актуальный код
git merge feature/new-feature #объединяем
git push origin main #пушим в репозиторий
```


**Удалить ветку (если больше не нужна)**
```bash
git branch -d feature/new-feature
```


**В общем**
main - это стабильная версия проекта, все остальные нововведения нужно разрабатывать в отдельных ветках, чтобы не сломать проект и после завершения отправлять изменения.
СРАЗУ ГОВОРЮ, НЕ НУЖНО ОТПРАВЛЯТЬ БАГИ В ВЕТКУ Main ПОЖАЛУЙСТА. так как у других разработчиков также будет этот баг

# License #
[License](https://catrob.at/licenses) of our project (mainly AGPL v3).
