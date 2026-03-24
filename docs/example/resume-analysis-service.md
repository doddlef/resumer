```java
    /**
 * 重新分析简历（手动重试）
 * 从数据库获取简历文本并发送分析任务
 *
 * @param resumeId 简历ID
 */
@Transactional
public void reanalyze(Long resumeId) {
    ResumeEntity resume = resumeRepository.findById(resumeId)
            .orElseThrow(() -> new BusinessException(ErrorCode.RESUME_NOT_FOUND, "简历不存在"));

    log.info("开始重新分析简历: resumeId={}, filename={}", resumeId, resume.getOriginalFilename());

    String resumeText = resume.getResumeText();
    if (resumeText == null || resumeText.trim().isEmpty()) {
        // 如果没有缓存的文本，尝试重新解析
        resumeText = parseService.downloadAndParseContent(resume.getStorageKey(), resume.getOriginalFilename());
        if (resumeText == null || resumeText.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.RESUME_PARSE_FAILED, "无法获取简历文本内容");
        }
        // 更新缓存的文本
        resume.setResumeText(resumeText);
    }

    // 更新状态为 PENDING
    resume.setAnalyzeStatus(AsyncTaskStatus.PENDING);
    resume.setAnalyzeError(null);
    resumeRepository.save(resume);

    // 发送分析任务到 Stream
    analyzeStreamProducer.sendAnalyzeTask(resumeId, resumeText);

    log.info("重新分析任务已发送: resumeId={}", resumeId);
}
```