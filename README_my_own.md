# «Ничего не падает» — работа с расписанием

Sessions.kt:
выбраны data class для хранения информации о занятиях по причине того что само занятие есть неизменяемый объект.
Кроме того выбор обоснован тем, что для data class переопределены:

equals() и hashCode() - логично равенство двух занятий при одинаковом содержимом, а не при одинаковом адресе в памяти;

toString() с именем класса и значениями свойств;

copy() для создания изменённой копии

Отдельно про data class Assessment и защиту коллекций.
Здесь у нас компромисс, т.к. data class + init гарантирует нам сохранение инвариантов при copy().
Но это же и становится проблемой: нет логичной защиты от изменения списка преподавателей черз MutableList.
private constructor + фабрика не помогает, потому что copy() его раскрывает.
Здесь стоит заглушка в виде аннотации @ConsistentCopyVisibility, которая убирает предупреждение компилятора в тестах.
Но она же делает копию приватной, по-сути блокируя assessment.copy(place = "Room 305").
Наша разумная цена за защиту коллекций.


Про переопределение в журналировании:

add и addAll — это два разных публичных метода интерфейса SessionRepository. Переопределение одного не перехватывает вызовы другого.
Вызов addAll без переопределения не в курсе о том, что надо что-то логировать.

### Bypassing checks with copy

`Assessment` has a private primary constructor and is annotated with
`@ConsistentCopyVisibility`. Therefore its generated `copy()` method is also
private and is not part of the public API.

The only public creation path is the companion-object `invoke` factory. It
copies the passed teacher collection with `toList()` and delegates to the
private constructor, where all invariants are checked by `require`.

## Failure messages

The parser returns structured failure reasons. A separate formatter converts
them into English messages. Values taken from the input line are escaped:
backslash as `\\`, double quote as `\"`, and control or non-ASCII characters
as `\uXXXX`.

### Required messages

- No `|` separator: `line N: invalid field separator`
- Wrong number of fields: `line N: got K fields, expected 5`
- Blank required field: `line N: empty required field "date"`
- Invalid date: `line N: invalid date "2026-13-45"`
- Unknown session type: `line N: unknown session type "SEMINAR"`
- Too few exam teachers:
  `line N: type "EXAM" requires at least 2 teachers, got 1`

### Additional messages

- Cancelled session with invalid location:
  `line N: type "CANCELLED" requires location "-"`
- Cancelled session with invalid teachers:
  `line N: type "CANCELLED" requires teachers "-"`
- Active session with `-` as location:
  `line N: location must not be "-"`
- Invalid teacher:
  `line N: teacher #1 must not be blank`
  `line N: teacher #1 must not have leading or trailing whitespace`
  `line N: teacher #1 must not contain control characters`
  `line N: teacher #1 must not be "-"`
- Duplicate teacher:
  `line N: duplicate teacher "Ivanov"`
- Control character in a field:
  `line N: topic must not contain control characters`
- Leading or trailing whitespace in a field:
  `line N: topic must not have leading or trailing whitespace`