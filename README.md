# МехМагнат 🧥✨

Онлайн-платформа для бронирования и аренды шуб премиум-класса.

![Status](https://img.shields.io/badge/status-in%20development-yellow)
![License](https://img.shields.io/badge/license-MIT-blue)
![Version](https://img.shields.io/badge/version-1.0.0-green)

## 📖 О проекте

**МехМагнат** — современный сервис аренды меховых изделий. Клиенты могут забронировать шубу онлайн на нужные даты, а владельцы — сдать изделия в аренду и получать доход.

> «Роскошь без компромиссов — бронируй сегодня, блистай завтра»

## 🚀 Быстрый старт

### Требования

- Node.js >= 18.x
- PostgreSQL >= 14.x
- npm >= 9.x или pnpm >= 8.x

### Установка

```bash
# Клонирование репозитория
git clone https://github.com/your-org/mehmagnat.git
cd mehmagnat

# Установка зависимостей
npm install

# Копирование переменных окружения
cp .env.example .env

# Запуск миграций БД
npm run db:migrate

# Запуск в режиме разработки
npm run dev