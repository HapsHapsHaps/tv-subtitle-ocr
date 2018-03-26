
Setup:
Requires Tesseract version 4 installed on system.


In some instances
LD_LIBRARY_PATH might not have been defined in the system.
It can depending on system be set as follows:
create config file at path: "/etc/ld.so.conf.d/<config name>.conf"
The content of file will be: "/usr/local/lib"
Then save and run the following command to have it take affect: "sudo ldconfig".

