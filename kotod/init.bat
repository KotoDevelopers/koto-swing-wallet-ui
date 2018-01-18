@echo off

SET kotodir=%appdata%\Koto
SET kotoconf=%kotodir%\koto.conf
SET sproutdir=%appdata%\ZcashParams
SET sprout-proving_url=http://d.koto.cash/sprout-proving.key?a=.js
SET sprout-verifying_url=http://d.koto.cash/sprout-verifying.key?a=.js
SET sprout-proving=%sproutdir%\sprout-proving.key
SET sprout-verifying=%sproutdir%\sprout-verifying.key
SET sprout-proving-hash=8bc20a7f013b2b58970cddd2e7ea028975c88ae7ceb9259a5344a16bc2c0eef7
SET sprout-verifying-hash=4bd498dae0aacfd8e98dc306338d017d9c08dd0918ead18172bd0aec2fc5df82


rem ###################################################
rem kotodir�����邩�`�F�b�N
if exist %kotodir% goto EXIST_KOTODIR
mkdir %kotodir%
:EXIST_KOTODIR


rem ###################################################
rem koto.conf�����邩�`�F�b�N
if exist %kotoconf% goto EXIST_KOTOCOIN
echo koto.conf���쐬���܂��B
echo rpcuser=%USERNAME%_%RANDOM% > %kotoconf%
echo rpcpassword=%USERNAME%_%RANDOM%_%RANDOM% >> %kotoconf%
:EXIST_KOTOCOIN


rem ###################################################
rem sproutdir�����邩�`�F�b�N
if exist %sproutdir% goto EXIST_SPROUTDIR
mkdir %sproutdir%
:EXIST_SPROUTDIR

rem ###################################################
rem sprout-proving�����邩�`�F�b�N
if exist %sprout-proving% goto EXIST_PROVING

:DOWNLOAD_PROVING
rem ����������_�E�����[�h
echo sprout-proving.key�̃_�E�����[�h���J�n���܂��B
bitsadmin.exe /TRANSFER DOWNLOAD_PROVING %sprout-proving_url% %sprout-proving%

:EXIST_PROVING
rem �L������n�b�V�����`�F�b�N
echo sprout-proving�̃n�b�V�����`�F�b�N��
@FOR /F "delims=/" %%i IN ('CERTUTIL -hashfile %sprout-proving% SHA256 ^| FIND /V ":"') DO @((SET hash_sha256=%%i))
set proving_sha256=%hash_sha256: =%

if not %proving_sha256%==%sprout-proving-hash% (
echo �n�b�V������v���܂���ł����B
del /Q %sprout-proving%
goto DOWNLOAD_PROVING
)
:END_PROVING


rem ###################################################
rem sprout-verifying�����邩�`�F�b�N
if exist %sprout-verifying% goto EXIST_VERIFYING

:DOWNLOAD_VERIFYING
rem ����������_�E�����[�h
echo sprout-verifying.key�̃_�E�����[�h���J�n���܂��B
bitsadmin.exe /TRANSFER DOWNLOAD_VERIFYING %sprout-verifying_url% %sprout-verifying%

:EXIST_VERIFYING
rem �L������n�b�V�����`�F�b�N
echo sprout-verifying�̃n�b�V�����`�F�b�N��
@FOR /F "delims=/" %%i IN ('CERTUTIL -hashfile %sprout-verifying% SHA256 ^| FIND /V ":"') DO @((SET hash_sha256=%%i))
set verifying_sha256=%hash_sha256: =%

if not %verifying_sha256%==%sprout-verifying-hash% (
echo �n�b�V������v���܂���ł����B
del /Q %sprout-verifying%
goto DOWNLOAD_VERIFYING
)
:END_VERIFYING


rem ###################################################
echo �����ݒ肪�I�����܂����B
echo �}�C�j���O�Ȃǂ��s���ꍇ�́A%kotoconf%�ɏ�����Ă��郆�[�U���A�p�X���[�h���m�F���Ă��������B
