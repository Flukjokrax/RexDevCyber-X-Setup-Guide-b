package com.example.data

import kotlinx.coroutines.flow.Flow

class TaskRepository(private val taskDao: TaskDao) {

    val allTasks: Flow<List<TaskEntity>> = taskDao.getAllTasksFlow()

    suspend fun updateTaskCompletion(id: Int, isCompleted: Boolean) {
        taskDao.updateTaskCompletion(id, isCompleted)
    }

    suspend fun updateTaskDetails(id: Int, title: String, description: String, isCompleted: Boolean) {
        taskDao.updateTaskDetails(id, title, description, isCompleted)
    }

    suspend fun resetAllTasks() {
        taskDao.resetAllTasks()
    }

    suspend fun initializeDatabaseIfEmpty() {
        val currentTasks = taskDao.getAllTasks()
        if (currentTasks.isEmpty()) {
            val initialTasks = listOf(
                TaskEntity(
                    id = 1,
                    category = "01 - Environment",
                    title = "ติดตั้ง Node.js & npm บนระบบ",
                    description = "ดาวน์โหลดและติดตั้ง Node.js v20.12+ และระบบจัดการแพลตฟอร์ม npm บนเครื่องของคุณสำหรับการพัฒนา",
                    isCompleted = false
                ),
                TaskEntity(
                    id = 2,
                    category = "01 - Environment",
                    title = "ตรวจสอบเวอร์ชัน Node.js & npm",
                    description = "รันตรวจสอบรุ่นสัญญาและแพ็คเกจว่าสอดคล้องสมบูรณ์ดีด้วยคำสั่ง node -v และ npm -v ผ่าน Terminal",
                    isCompleted = false
                ),
                TaskEntity(
                    id = 3,
                    category = "02 - Local Dev",
                    title = "เพิ่มและเรียกประมวลติดตั้งแพ็กเกจ",
                    description = "รันคำสั่ง npm install เพื่อสกัดและเตรียมความพร้อมของโมดูลย่อยที่จำเป็นทั้งหมดในไดเรกทอรีโครงการ",
                    isCompleted = false
                ),
                TaskEntity(
                    id = 4,
                    category = "02 - Local Dev",
                    title = "เปิดใช้เซิร์ฟเวอร์ท้องถิ่น",
                    description = "เปิดใช้นักพัฒนาโต้ตอบด้วยคำสั่ง npm run dev เปิดรันพอร์ต 3000 และทดสอบดูอินเทอร์เฟซเพื่อพัฒนาต่อ",
                    isCompleted = false
                ),
                TaskEntity(
                    id = 5,
                    category = "03 - Translations",
                    title = "ลงทะเบียนบัญชี Crowdin",
                    description = "เปิดบริการคลาวด์แปลของ Crowdin และจับคู่ภาษาแปลต้นทาง (เช่น อังกฤษ - ไทย) ให้กับแพ็กเกจเอกสารโครงการ",
                    isCompleted = false
                ),
                TaskEntity(
                    id = 6,
                    category = "03 - Translations",
                    title = "จัดทำโครงสร้างไฟล์ crowdin.yml",
                    description = "เขียนตั้งค่าไฟล์ดึงภาษาแปล crowdin.yml และระบุเส้นทางไฟล์ต้นฉบับกับรูปแบบจัดเก็บให้สอดคล้องกัน",
                    isCompleted = false
                ),
                TaskEntity(
                    id = 7,
                    category = "04 - Cloud Deploy",
                    title = "เชื่อมบัญชีผ่าน firebase login",
                    description = "ล็อกอินเชื่อมบัญชีนักพัฒนา Google Firebase ด้วยชุดเครื่องมือคอมมานด์ไลน์ firebase login ผ่านหน้าเว็บเบราว์เซอร์",
                    isCompleted = false
                ),
                TaskEntity(
                    id = 8,
                    category = "04 - Cloud Deploy",
                    title = "คอมไพล์ซอร์สโค้ด (npm run build)",
                    description = "สั่งการประกอบแพคไคลเอนต์หน้าเว็บและคอมไพล์โค้ดเป็น static เว็บไซน์ที่เร็วและบีบอัดเรียบร้อยในโฟลเดอร์ /dist",
                    isCompleted = false
                ),
                TaskEntity(
                    id = 9,
                    category = "04 - Cloud Deploy",
                    title = "Deploy ขึ้นคลาวด์ (firebase deploy)",
                    description = "รันคำสั่งเผยแพร่ออนไลน์และอัปโหลด static บิลด์ไปยัง Firebase CDN มอบหน้าพอร์ทัลพร้อมแชร์ทันที",
                    isCompleted = false
                )
            )
            taskDao.insertTasks(initialTasks)
        }
    }
}
