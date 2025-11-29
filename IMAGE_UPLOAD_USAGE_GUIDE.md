# Image Upload Feature Guide

This guide explains how to use the new dual image upload options for **Pins** and **Board Banners**.

---

## 🎯 Features Implemented

### 1. **Pin Creation with Dual Image Options**
- **Option A**: Upload image file from device (auto-fetches path using `ImageUtil`)
- **Option B**: Provide image URL directly

### 2. **Board Banner with Dual Image Options**
- **Option A**: Upload banner image file from device (auto-fetches path using `ImageUtil`)
- **Option B**: Provide banner image URL directly
- **Optional**: Banner image is not required for board creation

---

## 📝 API Usage Examples

### **Pin Creation**

#### **Endpoint**: `POST /pins`
**Content-Type**: `multipart/form-data`

#### **Option 1: Upload Image from Device**
```http
POST /pins
Headers:
  X-User-Id: user123
  Content-Type: multipart/form-data

Form Data:
  title: "Beautiful Sunset"
  description: "Amazing sunset view"
  boardId: "board123"
  visibility: "PUBLIC"
  sourceUrl: "https://example.com/source" (optional)
  image: [FILE] (image file from device)
```

#### **Option 2: Use Image URL**
```http
POST /pins
Headers:
  X-User-Id: user123
  Content-Type: multipart/form-data

Form Data:
  title: "Beautiful Sunset"
  description: "Amazing sunset view"
  boardId: "board123"
  visibility: "PUBLIC"
  sourceUrl: "https://example.com/source" (optional)
  imageUrl: "https://example.com/images/sunset.jpg"
```

#### **Option 3: Both Provided (imageUrl takes priority)**
```http
POST /pins
Headers:
  X-User-Id: user123
  Content-Type: multipart/form-data

Form Data:
  title: "Beautiful Sunset"
  description: "Amazing sunset view"
  boardId: "board123"
  visibility: "PUBLIC"
  image: [FILE]
  imageUrl: "https://example.com/images/sunset.jpg" (This will be used)
```

---

### **Board Creation with Banner**

#### **Endpoint**: `POST /boards`
**Content-Type**: `multipart/form-data`

#### **Option 1: Upload Banner from Device**
```http
POST /boards
Headers:
  X-User-Id: user123
  Content-Type: multipart/form-data

Form Data:
  name: "Travel Inspiration"
  description: "My favorite travel destinations"
  category: "Travel"
  visibility: "PUBLIC"
  bannerImage: [FILE] (banner image file from device)
```

#### **Option 2: Use Banner URL**
```http
POST /boards
Headers:
  X-User-Id: user123
  Content-Type: multipart/form-data

Form Data:
  name: "Travel Inspiration"
  description: "My favorite travel destinations"
  category: "Travel"
  visibility: "PUBLIC"
  bannerImageUrl: "https://example.com/banners/travel.jpg"
```

#### **Option 3: No Banner (Optional)**
```http
POST /boards
Headers:
  X-User-Id: user123
  Content-Type: multipart/form-data

Form Data:
  name: "Travel Inspiration"
  description: "My favorite travel destinations"
  category: "Travel"
  visibility: "PUBLIC"
  (no banner image or URL provided)
```

---

## 🔧 Backend Implementation Details

### **Pin Service Logic**

```java
// Priority order:
1. If imageUrl is provided → Use it directly
2. Else if image file is uploaded → Upload via FileUploadService
3. Else → Throw error (image is required for pins)
```

### **Board Service Logic**

```java
// Priority order:
1. If bannerImageUrl is provided → Use it directly
2. Else if bannerImage file is uploaded → Upload via FileUploadService
3. Else → Set coverImageUrl as null (banner is optional)
```

---

## 📋 Request Parameters

### **Pin Creation Parameters**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `title` | String | ✅ Yes | Pin title (max 200 chars) |
| `description` | String | ❌ No | Pin description (max 500 chars) |
| `boardId` | String | ✅ Yes | Target board ID |
| `visibility` | String | ✅ Yes | PUBLIC or PRIVATE |
| `sourceUrl` | String | ❌ No | Original source URL |
| `image` | File | ⚠️ Conditional | Image file (required if imageUrl not provided) |
| `imageUrl` | String | ⚠️ Conditional | Image URL (required if image file not provided) |

### **Board Creation Parameters**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `name` | String | ✅ Yes | Board name (max 100 chars) |
| `description` | String | ❌ No | Board description (max 200 chars) |
| `category` | String | ❌ No | Board category |
| `visibility` | String | ✅ Yes | PUBLIC or PRIVATE |
| `bannerImage` | File | ❌ No | Banner image file |
| `bannerImageUrl` | String | ❌ No | Banner image URL |

---

## 🎨 Frontend Implementation Examples

### **Using Fetch API (JavaScript)**

#### **Pin with File Upload**
```javascript
const formData = new FormData();
formData.append('title', 'My Pin');
formData.append('description', 'Pin description');
formData.append('boardId', 'board123');
formData.append('visibility', 'PUBLIC');
formData.append('image', fileInput.files[0]); // File from input

fetch('/pins', {
  method: 'POST',
  headers: {
    'X-User-Id': 'user123'
  },
  body: formData
})
.then(response => response.json())
.then(data => console.log(data));
```

#### **Pin with URL**
```javascript
const formData = new FormData();
formData.append('title', 'My Pin');
formData.append('description', 'Pin description');
formData.append('boardId', 'board123');
formData.append('visibility', 'PUBLIC');
formData.append('imageUrl', 'https://example.com/image.jpg');

fetch('/pins', {
  method: 'POST',
  headers: {
    'X-User-Id': 'user123'
  },
  body: formData
})
.then(response => response.json())
.then(data => console.log(data));
```

#### **Board with Banner Upload**
```javascript
const formData = new FormData();
formData.append('name', 'My Board');
formData.append('description', 'Board description');
formData.append('category', 'Travel');
formData.append('visibility', 'PUBLIC');
formData.append('bannerImage', bannerFileInput.files[0]); // File from input

fetch('/boards', {
  method: 'POST',
  headers: {
    'X-User-Id': 'user123'
  },
  body: formData
})
.then(response => response.json())
.then(data => console.log(data));
```

---

## ✅ Validation Rules

### **Image Uploads (FileUploadService)**
- **Allowed formats**: jpg, jpeg, png, gif, webp
- **Max file size**: 10 MB
- **Path format**: `/uploads/{uuid}.{extension}`

### **Image URLs**
- No validation on URL format (accepts any string)
- Frontend should validate URL format if needed

---

## 🚀 Testing with Postman

### **Test Pin Creation with File**
1. Create new POST request to `/pins`
2. Add header: `X-User-Id: your-user-id`
3. Select Body → form-data
4. Add fields as shown in examples
5. For `image`, select "File" type and choose file
6. Send request

### **Test Pin Creation with URL**
1. Same as above, but use `imageUrl` text field instead of `image` file
2. Provide a valid image URL

### **Test Board Creation**
1. Create new POST request to `/boards`
2. Add header: `X-User-Id: your-user-id`
3. Select Body → form-data
4. Add fields including optional `bannerImage` or `bannerImageUrl`
5. Send request

---

## 📊 Response Examples

### **Success Response (Pin)**
```json
{
  "status": "success",
  "message": "Pin created successfully",
  "data": {
    "pinId": "generated-pin-id",
    "title": "Beautiful Sunset",
    "description": "Amazing sunset view",
    "imageUrl": "/uploads/uuid.jpg",
    "boardId": "board123",
    "visibility": "PUBLIC",
    "createdAt": "2025-11-30T00:00:00",
    "createdBy": {
      "userId": "user123",
      "username": "john_doe"
    }
  }
}
```

### **Success Response (Board)**
```json
{
  "status": "success",
  "message": "Board created successfully",
  "data": {
    "boardId": "generated-board-id",
    "name": "Travel Inspiration",
    "description": "My favorite travel destinations",
    "category": "Travel",
    "coverImageUrl": "/uploads/uuid.jpg",
    "visibility": "PUBLIC",
    "pinCount": 0,
    "createdAt": "2025-11-30T00:00:00"
  }
}
```

### **Error Response (No Image Provided for Pin)**
```json
{
  "status": "error",
  "message": "Either image file or image URL must be provided",
  "timestamp": "2025-11-30T00:00:00"
}
```

---

## 🎯 Summary

- ✅ **Pins**: MUST have an image (either file upload or URL)
- ✅ **Boards**: Banner is OPTIONAL (can use file upload, URL, or none)
- ✅ **Priority**: If both file and URL are provided, URL takes precedence
- ✅ **Auto-path**: FileUploadService automatically handles file uploads and returns path
- ✅ **Validation**: File uploads are validated (size, format) by FileUploadService

---

## 📞 Support

For issues or questions, please refer to the main project documentation or contact the development team.
