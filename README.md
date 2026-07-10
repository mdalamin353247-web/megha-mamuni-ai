# মেঘা মামুনি (Megha Mamuni) - AI Assistant Android App

## বাংলা | Bengali

### প্রকল্প পরিচিতি
মেঘা মামুনি একটি স্মার্ট, সুন্দর এবং কিউট AI অ্যাসিস্ট্যান্ট Android অ্যাপ যা কম-দামি Android ফোনেও (২ জিবি র‍্যাম, Android 11) ভালোভাবে চলে।

### মূল ফিচার
- ✅ বাংলা + ইংরেজি কথোপকথন
- ✅ ভয়েস চ্যাট (Speech-to-Text + Text-to-Speech)
- ✅ কিউট অ্যানিমেটেড গার্ল অ্যাভাটার
- ✅ ওয়েক ওয়ার্ড: "মেঘা মামুনি" / "Hey Megha"
- ✅ ফ্ল্যাশলাইট, ব্যাটারি, অ্যালার্ম, রিমাইন্ডার
- ✅ আবহাওয়া, ক্যালকুলেটর, ইন্টারনেট সার্চ
- ✅ ডার্ক মোড / লাইট মোড
- ✅ পিন + ফিঙ্গারপ্রিন্ট সিকিউরিটি
- ✅ লো-এন্ড ডিভাইস অপ্টিমাইজেশন

---

## English

### Project Overview
Megha Mamuni is a smart, beautiful, and cute AI assistant Android app optimized for low-end devices (2GB RAM, Android 11+).

### Features
- Natural AI conversation (Bangla + English)
- Voice chat with female voice (TTS pitch 1.2f)
- Custom Canvas-drawn animated cartoon avatar
- Wake word: "মেঘা মামুনি" / "Hey Megha"
- Smart commands: flashlight, battery, alarm, apps
- Weather, calculator, internet search
- Dark/Light mode
- PIN + Fingerprint security
- Low-end device optimization (2GB RAM check)

---

## Setup Instructions

### 1. API Keys Required
```
OpenAI API Key    → AIEngine.kt → replace "YOUR_OPENAI_API_KEY"
OpenWeather Key   → AssistantCommands.kt → replace "YOUR_WEATHER_API_KEY"
```

### 2. Build Requirements
- Android Studio Hedgehog or newer
- Min SDK: 30 (Android 11)
- Target SDK: 34
- Kotlin 1.9+

### 3. Build Steps
```bash
git clone https://github.com/megha/megha-mamuni-ai
cd megha-mamuni-ai
# Open in Android Studio
# Add API keys
# Build > Generate Signed APK
```

---

## Architecture
```
MVVM Architecture
├── UI Layer       → Activities, Fragments, Custom Views
├── ViewModel      → ChatViewModel, VoiceViewModel  
├── Repository     → ChatRepository (Room DB + SharedPrefs)
└── Remote         → AIEngine (OpenAI API via Retrofit)
```

## File Structure
```
app/
├── build.gradle
├── src/main/
│   ├── AndroidManifest.xml
│   ├── java/com/meghamamuni/
│   │   ├── app/
│   │   │   └── MainActivity.kt
│   │   └── assistant/
│   │       ├── AIEngine.kt
│   │       ├── VoiceManager.kt
│   │       ├── AssistantCommands.kt
│   │       ├── ChatRepository.kt
│   │       ├── security/SecurityManager.kt
│   │       ├── service/WakeWordService.kt
│   │       ├── theme/ThemeManager.kt
│   │       └── ui/
│   │           ├── SplashActivity.kt
│   │           ├── SettingsActivity.kt
│   │           ├── AvatarView.kt
│   │           ├── ChatAdapter.kt
│   │           └── AnimationHelper.kt
│   └── res/
│       ├── layout/ (all XML layouts)
│       ├── drawable/ (icons, bubbles)
│       ├── values/ (colors, strings)
│       └── menu/ (bottom nav)
```

## Credits
- Developed by: MdRana MsPakhi (Shohel Rana)
- AI Engine: OpenAI GPT-3.5-turbo
- Avatar: Custom Canvas Drawing (Kotlin)
- Built with ❤️ using Vesper AI Assistant
```
