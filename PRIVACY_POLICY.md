# SmartFind - Privacy Policy

**Effective Date:** January 1, 2024  
**Last Updated:** January 1, 2024

---

## Introduction

SmartFind ("we," "our," "the App") is committed to protecting your privacy. This Privacy Policy explains how SmartFind collects, uses, stores, and protects your information.

**Key Principle:** SmartFind is an **offline-first, privacy-focused** app. All data remains on your device. We do not collect, transmit, or sell your personal information.

---

## Data We Collect

### 1. Images
- **What:** Photos captured when you use the object detection feature
- **Why:** To detect objects and maintain a history of detections
- **Storage:** Saved locally in app-specific storage on your device
- **Sharing:** Never uploaded to servers or shared with third parties

### 2. Location Data (Optional)
- **What:** GPS coordinates (latitude, longitude, accuracy)
- **Why:** To help you remember where objects were detected
- **Storage:** Saved locally in the app database
- **When:** Only captured when you actively use detection with location permission granted
- **Sharing:** Never transmitted to external servers

### 3. Detection Metadata
- **What:** Object names, confidence scores, timestamps
- **Why:** To provide search and history features
- **Storage:** Saved locally in Room database
- **Sharing:** Never shared externally

---

## How We Use Your Data

- **Object Detection:** Images are analyzed on-device using TensorFlow Lite
- **History Management:** Data is stored to provide a searchable history
- **Location Tagging:** Optional GPS coordinates help you remember detection locations
- **CSV Export:** You can export your data for backup or analysis

**We do NOT:**
- Upload your data to cloud servers
- Share your data with third parties
- Use your data for advertising
- Track your behavior outside the app

---

## Data Storage

### Local Storage
All data is stored in Android's app-specific directories:
- **Images:** `/Android/data/com.smartfind.app/files/Pictures/SmartFind/`
- **Database:** Internal app database (Room SQLite)
- **Preferences:** SharedPreferences (settings only)

### Security
- Data is protected by Android's application sandbox
- Other apps cannot access your SmartFind data
- Data is automatically removed when you uninstall the app

---

## Permissions

### Required Permissions

**Camera** (`android.permission.CAMERA`)
- **Purpose:** Capture images for object detection
- **When:** Requested on first use of detection feature
- **Fallback:** App cannot function without camera access

**Storage** (`WRITE_EXTERNAL_STORAGE` - Android 9 and below only)
- **Purpose:** Save images to device storage
- **When:** Requested on first detection save
- **Note:** Not required on Android 10+ (scoped storage)

### Optional Permissions

**Location** (`ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`)
- **Purpose:** Tag detections with location (optional feature)
- **When:** Requested when you enable location tagging
- **Fallback:** Detection works without location permission

**Internet** (`android.permission.INTERNET`)
- **Purpose:** Reverse geocoding (convert GPS to address) - optional
- **Note:** Detection works offline; internet only needed for address lookup
- **Data Sent:** Only GPS coordinates to Geocoder API (standard Android API)

---

## Third-Party Services

### TensorFlow Lite
- **Purpose:** On-device machine learning inference
- **Data Processing:** All processing happens on your device
- **Data Sharing:** No data sent to Google or TensorFlow servers

### Google Play Services Location
- **Purpose:** GPS coordinate retrieval
- **Data Sharing:** Standard Android API; no data sent to Google for SmartFind's purposes

### Geocoder API (Optional)
- **Purpose:** Convert GPS coordinates to human-readable addresses
- **Data Sent:** GPS coordinates only (when you enable location)
- **Data Received:** Address string
- **Privacy:** Google may log requests per their privacy policy

---

## Data Retention

### Automatic Cleanup
- Detections older than **30 days** are automatically deleted by WorkManager
- You can adjust retention in Settings (7, 14, 30, or 60 days)

### Manual Deletion
- **Individual detections:** Swipe to delete in History screen
- **Bulk deletion:** Settings → Clear Old Files
- **All data:** Uninstall the app

---

## Your Rights

You have complete control over your data:

1. **Access:** View all detections in the History screen
2. **Export:** Export data to CSV via Settings → Export to CSV
3. **Delete:** Delete specific detections or all data
4. **Portability:** Export CSV for use in other applications

---

## Children's Privacy

SmartFind does not knowingly collect data from children under 13. The app is rated for all ages and does not require account creation or personal information.

---

## Changes to This Policy

We may update this Privacy Policy from time to time. Changes will be posted in the app and on our website. Continued use of the app after changes constitutes acceptance.

---

## Data Breach Notification

In the unlikely event of a data breach:
- We will notify you within 72 hours
- We will describe the nature of the breach
- We will provide steps to mitigate harm

**Note:** Since all data is stored locally, a breach would require physical device access or malware.

---

## International Users

SmartFind stores all data locally on your device. No data crosses international borders unless you manually export and transfer it.

---

## Contact Us

If you have questions about this Privacy Policy or data practices:

- **Email:** [Your email address]
- **GitHub:** [Repository issues URL]
- **Address:** [Optional physical address]

---

## Legal Compliance

SmartFind complies with:
- **GDPR** (General Data Protection Regulation) - EU
- **CCPA** (California Consumer Privacy Act) - USA
- **COPPA** (Children's Online Privacy Protection Act) - USA
- **Android Privacy Guidelines**

---

## Consent

By using SmartFind, you consent to this Privacy Policy. You can withdraw consent by uninstalling the app, which will delete all local data.

---

**Last Updated:** January 1, 2024  
**Version:** 1.0.0
