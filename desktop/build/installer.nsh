!macro customInit
  IfFileExists "D:\" 0 +2
  StrCpy $INSTDIR "D:\DiaryApp\Desktop"
!macroend
