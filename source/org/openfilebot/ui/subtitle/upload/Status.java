package org.openfilebot.ui.subtitle.upload;

enum Status {
	IllegalInput, CheckPending, Checking, CheckFailed, AlreadyExists, Identifying, IdentificationRequired, UploadReady, Uploading, UploadComplete, UploadFailed;
}