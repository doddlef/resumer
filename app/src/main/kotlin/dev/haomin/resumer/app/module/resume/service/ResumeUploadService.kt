package dev.haomin.resumer.app.module.resume.service

import dev.haomin.resumer.app.module.resume.service.dto.ResumeUploadCmd
import dev.haomin.resumer.app.module.resume.service.dto.ResumeUploadResult

interface ResumeUploadService {

    /**
     * Upload a resume and start a task to analyze it. If the resume is a duplicate,
     * it will not be analyzed and the result will indicate that it is a duplicate.
     *
     * @param cmd the command containing the file and account ID
     * @return the result of the upload and analysis, including whether it is a duplicate
     */
    fun uploadAndAnalyze(cmd: ResumeUploadCmd): ResumeUploadResult
}