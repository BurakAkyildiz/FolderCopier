import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;





/**
 *İstenilen klasör veya dosyayı hedef olarak istenen klasöre kopyalayan program.
 * @author Burak Akyıldız
 */
public class FolderCopier
{
//------------------------------------INSTANCE VARIABLES-----------------------------------------
    
    File fileSource = null,fileDestination = null;
    Long allSize = 0L, allReadedSize = 0L;
    int anlikOkuma = 0;
    ArrayList<File> selectedFileList = new ArrayList<>();
    
//-----------------------------------------------------------------------------------------------------

    

    
    
    
    /**Kullanıcıdan Kopyalanacak dosya yolu ve hedef dosya yolunu okuyan metod.
        * Eğer kullanıcı boş değer girerse program kendini kapatır.
        * Eğer kullanıcı hatalı dosya yolu girerse tekrar dosya yolu girmesini ister.
        */
    public void readFilePath()
    {
        Scanner scan = new Scanner(System.in,"utf-8");
        String filePathStr_Source = "",filePathStr_Destination = "";
       
        while(true)//Kopyalanacak dosya veya klasör yolunun kullanıcıdan alınması
        {
            System.out.print("Kopyalanacak Dosya Yada Klasör Yolunu Girin :");
            filePathStr_Source = scan.nextLine();
            
            if(filePathStr_Source == null || filePathStr_Source.isEmpty()) // Kullanıcı değer girmeden enter a basarsa program kapatılır.
                System.exit(1);
            
            fileSource = new File(filePathStr_Source);
            try 
            {
                if(fileSource.isDirectory() || fileSource.isFile())// Dosyanın mevcut olup olmadığı kontrol edilir. Mevcut değilse tekrar değer girmesi istenir.
                    break;
                else
                {
                    System.err.println("Kopyalanacak Dosya Yolu Hatalı !");
                    fileSource = null;
                } 
                
            }catch (Exception e) {}
        }
        
        
        while(true)//Hedef dosya yolununun kullanıcıdan alınması
        {
            System.out.print("Hedef Klasör Yolunu Girin :");
            filePathStr_Destination = scan.nextLine();
            
            if(filePathStr_Destination == null || filePathStr_Destination.isEmpty()) // Kullanıcı değer girmeden enter a basarsa program kapatılır.
                System.exit(1);
            
            fileDestination = new File(filePathStr_Destination);
            
            try 
            {
                fileDestination.mkdirs();
                if(fileDestination.isDirectory())// Dosya yolunun Sisteme uygun olup olmadığını kontrol eder. Eğer uygun değilse tekrar veri yolu istenilir.
                    break;
                else
                {
                    System.err.println("Hedef Dosya Yolu Hatalı !");
                    fileDestination = null;
                }
                
            }catch (Exception e) {}
        }
        
        allSize = selectSourceFiles(fileSource);
        System.out.println("Bilgi : "+selectedFileList.size()+" File | "+byteToMb(allSize)+" MB");
    }
    
    
    /**
     * file Klasörünün içerisindeki bütün dosyaları selectedFileList listesine ekler.
     * İç içe klasörleri tarar eğer ulaşılan file bir dosya ise listeye ekler ve boyutunu size değişkeninde toplar
     * eğer ulaşılan bir klasörse iç klasörlerini kendisini çağırarak tarar.
     * @param file
     * @return file size
     */
    private long selectSourceFiles(File file)
    {
        long size = 0;
        
        if(file.isDirectory())//Eğer file bir klasörse iç dosyaları taranır.
        {
            File[] innerFileList = file.listFiles();
            if(innerFileList == null) return 0;//Eğer klasörün içerisi boşsa 0 boyut döndürülür.
            
            for (File innerFile : innerFileList) {
                size += selectSourceFiles(innerFile);
            }
            

        }else if(file.isFile())//Eğer bir dosya ise listelenir ve boyutu toplanır.
        {
           selectedFileList.add(file);
           size += file.length();
        }   
        
        
        return size;
    }
    
    
    /**selectedFileList listesinde kayıtlı dosyaları fileDestination klasörüne kopyalar.
     * Kaynak dosyadaki dosyalama sistemini hedef dosyada oluşturarak kopyalama işlemi yapılır.
     * Kopyalama işlemi esnasında verilerin kopyalanma durumu System.out çıktı yoluna yazılır.
        * 
        * Eğer selectedFileList ile seçilmiş dosyalardan birisi herhangi bir şekilde silinmiş veya taşınmışsa
        * hata yazısı yazar ve sıradaki dosya aktarımına geçilir.
        * 
        * @return eğer kopyalama başarılı olursa true değer döndürülür. Eğer Hedef dosya yolu oluşturulmadıysa false değer döndürür.
        */
    public boolean coppySelectedFiles()
    {
        System.out.println("---------------- KOPYALAMA BAŞLIYOR ----------------");
        if(fileDestination == null)//Hedef dosya denetlenir. Eğer tanımlıysa dosya yolu oluşturulur.
            return false;
        fileDestination.mkdirs();
        
        for (File selectedFile : selectedFileList) //selectSourcFiles() metodu ile seçilmiş olan bütün dosyaları sırasıyla kopyalar.
        {
            File myFile = setupFilePath(selectedFile);//kopyalanacak dosyanın yolunun belirlenmesi ve dosyanın oluşturulması.
            
            if(myFile != null)//eğer dosya oluşturulduysa kopyalama işlemini mevcut dosya için başlatır.
            copyFile(selectedFile,myFile);
            
        }
        return true;
    }
    
    
    /**
     * Alınan parametrelere göre selectedFile dosyasını newFile dosyasına kopyalayan metod.
     * parametreler mevcut dosya olmalıdır. Klasör olmamalıdır.
     * Gönderme işlemi esnasında;<FileNotFoundException> <IOException> hataları gelirse kullanıcı bilgilendirilir kopyalama iptal edilir. Mevcut streamler kapatılır. Kopyalanan dosya boyutuna iptal edilen hedef boyutta eklenir.
     * showCurrentState() metodu ile kullanıcı bilgilendirilir.
     * @param selectedFile kaynak dosya
     * @param newFile kopya dosya
     */
    private void copyFile(File selectedFile, File newFile)
    {
            FileInputStream fIn = null;
            FileOutputStream fOut = null;
            boolean isFileCopied = false;
            try {//myFile dosyasına 32kb veri büyüklüğüyle verilerin kopyalanması sağlanır.
                
                fIn = new FileInputStream(selectedFile.getAbsolutePath());
                fOut = new FileOutputStream(newFile.getAbsolutePath());
                isFileCopied = false;
                byte[] tmp = new byte[1024 * 32];
                anlikOkuma = 0;
                
                
                while((anlikOkuma = fIn.read(tmp)) != -1)//Okunan her 32kb lık veriyi dosya sonu gelene kadar yazar.
                {
                    fOut.write(tmp,0,anlikOkuma);
                    allReadedSize += anlikOkuma;
                }
                isFileCopied = true;
                fOut.close();
                fIn.close();
                
                
                showCurrentState(newFile.getAbsolutePath(),selectedFile.length());//Her dosya bitiminde konsola bilgi mesajı yazılır.
                
            }
            catch(Exception e){//Hata ile karşılaşılması durumunda mevcut dosyayı atlar. allSize takibinin düzgün olabilmesi için iptal olan dosya boyutu eklenir.
                e.printStackTrace();
                System.err.println(e.getMessage());
                allSize += selectedFile.length();
                try {
                    if(!isFileCopied)//eğer kopyalama işlemi yarıda kesilirse hasarlı dosya bırakmamak için yarım aktarılan dosya silinir.
                    {
                        if(newFile.delete())
                        {System.out.println("Damaged file is deleted :"+newFile.getAbsolutePath());}    
                    }
                    
                    if(fIn != null)//Streamler güvenlik amacıyla elle kapatılır.
                        fIn.close();
                    if(fOut != null)
                        fOut.close();
                }
                catch (IOException ex) {}
                if(e.getMessage().toLowerCase().contains("access is denied"))
                {
                    System.err.println("Dosya Yoluna Erişim Engellendi ! "+selectedFile.getAbsolutePath());
                }
                else if(e instanceof FileNotFoundException)
                    System.err.println(selectedFile.getAbsolutePath()+" Dosyası bulunamadı !");
                else if(e instanceof IOException)
                    System.err.println("Dosya Yazılamadı !");
                
            }
            
    }
    
    /**
     * Konsola mevcut kopyalama işlemi hakkında bilgi mesajı gönderen metod.
     * @param newFilePathStr kopyalanmış olan dosya yolu.
     * @param fileLength kopyalanmış olan dosya boyutu.
     */
    private void showCurrentState(String newFilePathStr,long fileLength)
    {
         //Kullanıcının bilgilendirilmesi
        String bilgiMsg = "%%%-3.2f |  %s / %s (mb) Kopyalandı  |  %s %s";

        String msg  = String.format(bilgiMsg,((float)allReadedSize / allSize * 100)//Durum yüzdesi
                                            ,byteToMb(allReadedSize)//toplam okunan boyut
                                            ,byteToMb(allSize)//toplam boyut
                                            ,newFilePathStr//kopyanın oluşturulduğu dosya
                                            ,"[ "+byteToMb(fileLength)+" mb ]");//kopyalanan dosya boyutu
        System.out.println(msg);
    }
    
    
    /**
     * selectedfile , fileSource, fileDestination değişkenleri baz alınarak yeni file için dosya yolundaki klasörleri açar ve file' ı oluşturur.
     * Eğer dosya yolu mevcutsa oluşturmaz.
     * Eğer dosya önceden mevcutsa mevcut dosyayı siler tekrar oluşturur.
     * @param selectedFile
     * @return 
     */
    private File setupFilePath(File selectedFile)
    {
        
        String fileSourceFolderPath  = fileSource.isDirectory() ? fileSource.getAbsolutePath() : fileSource.getParent();
            
        File myFile = new File(""+fileDestination.getAbsolutePath()
                                 +selectedFile.getAbsolutePath().toString().substring(fileSourceFolderPath.length()).trim());//Hedef dosyayı kopyalanacak dosyadaki tree yapısına göre tanımlar.

        if(!myFile.getParentFile().canRead())//Tanımlanan dosyanın alt klasörde olması ihtimaliyle gerekli alt klasörler oluşturulur.
        {
            myFile.getParentFile().mkdirs();
        }

        if(myFile.isFile())//kopyalanacak dosya önceden belirlenen yolda mevcutsa hataları engellemek amacı ile sıfırlanır.
        {
            myFile.delete();

            try {
                myFile.createNewFile();
            }
            catch (IOException ex) {System.err.println("Kopyalanacak dosya hedef dosyada oluşturulamadı !");}
        }

        return myFile;
    }
    
    
    /**
     * Dosya boyutunu byte bazından mb bazına çeviren metod.
     * @param num byte cinsinden dosya boyutu
     * @return formtlı mb cinsinden boyut.
     */
    public static String byteToMb(long num)
    {
        long mbSize = num/1024/1024;
        
        double decimal = (num / (1024.0*1024.0)) % 1 ;
        
        String str = mbSize + "," +(""+decimal+"000").substring(2,5);
        
        return formatNumber(str);
    }
    
    
    /**
     * String olarak alınan sayısal double değerin virgül ve noktalamasının eklenmesini sağlayan metod.
     * @param num: Mb cinsinden double değerin String hali
     * @return fortmated double number
     */
    public static String formatNumber(String num)
    {
        StringBuilder strBuild = new StringBuilder();
        char[] numC = num.toCharArray();
        boolean isStartOfNumber = false;
        int counter = 0;
        for (int i = numC.length-1; i >= 0 ; i--) {
            
            strBuild.append(numC[i]);
            
            if(numC[i] == ',')
            {
                isStartOfNumber = true;
                continue;
            }
            
            if(isStartOfNumber)
            {
                counter++;
                if(counter % 3 == 0 && numC.length-1 > numC.length -1 - i)
                    strBuild.append(".");
            }
            
        }
        
        return strBuild.reverse().toString();
    }
    
    
    
    
    public static void main(String[] args) 
    {
        FolderCopier f = new FolderCopier();
        
        f.readFilePath();
        
        boolean isDone = f.coppySelectedFiles();
        if(isDone)
            System.out.println("---------------- Kopyalama İşlemi Tamamlandı ! ----------------");
        else
            System.out.println("@@@@@@@@ Kopyalama İşlemi İptal Edildi ! @@@@@@@@");
    }
    
}