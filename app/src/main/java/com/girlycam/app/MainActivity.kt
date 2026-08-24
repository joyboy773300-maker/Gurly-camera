package com.girlycam.app

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

val CREAM=Color(0xFFFFF9F5); val PINK=Color(0xFFF58AA8); val DEEP=Color(0xFFD85C7E); val BLUSH=Color(0xFFFCE8EE); val LAV=Color(0xFFEEE7FA); val TXT=Color(0xFF3C3035); val MUTED=Color(0xFF8C7B82)
enum class Page{HOME,CAMERA,EDITOR,TEMPLATES,COLLAGE}
enum class T(val label:String){POLAROID("Blush Polaroid"),FILM("Pink Film"),LAVENDER("Lavender Dream"),GINGHAM("Soft Gingham"),MINIMAL("Clean Minimal"),BOW("Bow Frame")}
data class Edit(val uri:Uri,val template:T=T.POLAROID,val ratio:Float=.8f)

class MainActivity:ComponentActivity(){
 override fun onCreate(b:Bundle?){super.onCreate(b);setContent{App()}}
}

@Composable fun App(){
 var page by remember{mutableStateOf(Page.HOME)}; var edit by remember{mutableStateOf<Edit?>(null)}; var collage by remember{mutableStateOf<List<Uri>>(emptyList())}; val ctx=LocalContext.current
 val gallery=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()){u->u?.let{edit=Edit(it);page=Page.EDITOR}}
 val multi=rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()){u->if(u.isNotEmpty()){collage=u.take(4);page=Page.COLLAGE}}
 MaterialTheme(colorScheme=lightColorScheme(background=CREAM,surface=Color.White,primary=PINK,onPrimary=Color.White,onBackground=TXT,onSurface=TXT)){
  Surface(Modifier.fillMaxSize(),color=CREAM){AnimatedContent(page,label="page"){p->when(p){Page.HOME->Home({page=Page.CAMERA},{gallery.launch("image/*")},{page=Page.TEMPLATES},{multi.launch(arrayOf("image/*"))});Page.CAMERA->Camera({page=Page.HOME}){u->edit=Edit(u);page=Page.EDITOR};Page.EDITOR->edit?.let{Editor(it,{page=Page.HOME}){edit=it}{save(ctx,it)}};Page.TEMPLATES->Templates({page=Page.HOME}){t->edit?.let{edit=it.copy(template=t);page=Page.EDITOR}};Page.COLLAGE->Collage(collage,{page=Page.HOME}){saveCollage(ctx,collage)}}}}
 }
}

@Composable fun Home(cam:()->Unit,gal:()->Unit,temp:()->Unit,col:()->Unit){Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp)){Spacer(Modifier.height(26.dp));Text("GirlyCam",fontSize=32.sp,fontWeight=FontWeight.SemiBold,color=DEEP);Text("capture  •  frame  •  shine",fontSize=13.sp,color=MUTED);Spacer(Modifier.height(25.dp));CardX(Icons.Rounded.PhotoCamera,"Camera","Take a new photo",cam);CardX(Icons.Rounded.PhotoLibrary,"Gallery","Pick a photo to edit",gal);CardX(Icons.Rounded.AutoAwesome,"Templates","Polaroid, film, bows & more",temp);CardX(Icons.Rounded.GridView,"Collage","Make a 2–4 photo collage",col);Spacer(Modifier.height(22.dp));Text("Your aesthetic toolkit",fontWeight=FontWeight.SemiBold);Spacer(Modifier.height(10.dp));Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){listOf("♡" to "Cute","✦" to "Frames","↗" to "Share").forEach{(a,b)->Column(Modifier.width(92.dp).clip(RoundedCornerShape(18.dp)).background(Color.White).padding(13.dp),horizontalAlignment=Alignment.CenterHorizontally){Text(a,color=PINK,fontSize=22.sp);Text(b,fontSize=11.sp,color=MUTED)}}};Spacer(Modifier.height(25.dp));Text("made with ♡",color=MUTED,fontSize=12.sp)}}
@Composable fun CardX(i:androidx.compose.ui.graphics.vector.ImageVector,t:String,s:String,click:()->Unit){Row(Modifier.fillMaxWidth().padding(vertical=6.dp).clip(RoundedCornerShape(23.dp)).background(Color.White).border(1.dp,BLUSH,RoundedCornerShape(23.dp)).clickable{click()}.padding(17.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(50.dp).clip(RoundedCornerShape(17.dp)).background(BLUSH),contentAlignment=Alignment.Center){Icon(i,null,tint=PINK)};Spacer(Modifier.width(15.dp));Column(Modifier.weight(1f)){Text(t,fontSize=16.sp,fontWeight=FontWeight.Medium);Text(s,fontSize=12.sp,color=MUTED)};Icon(Icons.Rounded.ChevronRight,null,tint=MUTED)}}

@Composable fun Camera(back:()->Unit,captured:(Uri)->Unit){val ctx=LocalContext.current;val owner=LocalLifecycleOwner.current;val pv=remember{PreviewView(ctx)};var cap by remember{mutableStateOf<ImageCapture?>(null)};val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){ };LaunchedEffect(Unit){if(ctx.checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED)permission.launch(Manifest.permission.CAMERA);val provider=ProcessCameraProvider.getInstance(ctx).get();val preview=Preview.Builder().build().also{it.surfaceProvider=pv.surfaceProvider};val c=ImageCapture.Builder().build();cap=c;provider.unbindAll();provider.bindToLifecycle(owner,CameraSelector.DEFAULT_BACK_CAMERA,preview,c)};Box(Modifier.fillMaxSize().background(Color.Black)){AndroidView({pv},Modifier.fillMaxSize());Row(Modifier.fillMaxWidth().padding(18.dp),horizontalArrangement=Arrangement.SpaceBetween){IconButton(back,Modifier.background(Color.White.copy(.85f),CircleShape)){Icon(Icons.Rounded.Close,null,tint=TXT)};Surface(color=Color.White.copy(.88f),shape=RoundedCornerShape(30.dp)){Text("4:5",Modifier.padding(horizontal=15.dp,vertical=9.dp),fontSize=12.sp)}};Column(Modifier.align(Alignment.BottomCenter).padding(bottom=32.dp),horizontalAlignment=Alignment.CenterHorizontally){Text("tap to capture ♡",color=Color.White,fontSize=12.sp);Spacer(Modifier.height(12.dp));Box(Modifier.size(82.dp).clip(CircleShape).background(Color.White).border(7.dp,PINK.copy(.5f),CircleShape).clickable{val c=cap?:return@clickable;val f=File(ctx.cacheDir,"photo_${System.currentTimeMillis()}.jpg");val o=ImageCapture.OutputFileOptions.Builder(f).build();c.takePicture(o,androidx.core.content.ContextCompat.getMainExecutor(ctx),object:ImageCapture.OnImageSavedCallback{override fun onError(e:ImageCaptureException){};override fun onImageSaved(r:ImageCapture.OutputFileResults){captured(Uri.fromFile(f))}})},contentAlignment=Alignment.Center){Box(Modifier.size(64.dp).clip(CircleShape).background(PINK))}}}}

@Composable fun Editor(e:Edit,back:()->Unit,change:(Edit)->Unit,save:()->Unit){Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp)){Row(verticalAlignment=Alignment.CenterVertically){IconButton(back){Icon(Icons.Rounded.ArrowBack,null)};Text("Editor",fontSize=25.sp,fontWeight=FontWeight.SemiBold,Modifier.weight(1f));Button(save,shape=RoundedCornerShape(15.dp),contentPadding=PaddingValues(horizontal=15.dp,vertical=8.dp)){Icon(Icons.Rounded.Download,null,Modifier.size(17.dp));Spacer(Modifier.width(4.dp));Text("Save")}};Spacer(Modifier.height(10.dp));Preview(e);Spacer(Modifier.height(17.dp));Text("Templates",fontWeight=FontWeight.SemiBold);Spacer(Modifier.height(8.dp));LazyRow(horizontalArrangement=Arrangement.spacedBy(9.dp)){items(T.entries){t->Column(Modifier.width(82.dp).clip(RoundedCornerShape(15.dp)).background(if(e.template==t)BLUSH else Color.White).border(1.dp,if(e.template==t)PINK else BLUSH,RoundedCornerShape(15.dp)).clickable{change(e.copy(template=t))}.padding(7.dp)){Box(Modifier.fillMaxWidth().aspectRatio(.8f).clip(RoundedCornerShape(9.dp)).background(bg(t)),contentAlignment=Alignment.Center){Text(if(t==T.BOW)"🎀" else "✦",color=PINK,fontSize=20.sp)};Text(t.label,fontSize=9.sp,maxLines=1)}}};Spacer(Modifier.height(17.dp));Text("Ratio",fontWeight=FontWeight.SemiBold);Spacer(Modifier.height(8.dp));Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("1:1" to 1f,"4:5" to .8f,"3:4" to .75f,"9:16" to .5625f).forEach{(l,r)->Surface(onClick={change(e.copy(ratio=r))},color=if(e.ratio==r)PINK else Color.White,contentColor=if(e.ratio==r)Color.White else TXT,shape=RoundedCornerShape(13.dp),border=BorderStroke(1.dp,if(e.ratio==r)PINK else BLUSH)){Text(l,Modifier.padding(horizontal=14.dp,vertical=9.dp),fontSize=12.sp)}}};Spacer(Modifier.height(20.dp));Text("Stickers, filters and text are next in the editor pipeline.",fontSize=12.sp,color=MUTED)}}
@Composable fun Preview(e:Edit){Box(Modifier.fillMaxWidth().aspectRatio(e.ratio).clip(RoundedCornerShape(25.dp)).background(bg(e.template))){AsyncImage(e.uri,null,Modifier.fillMaxSize().padding(if(e.template==T.POLAROID)22.dp else 12.dp).clip(RoundedCornerShape(4.dp)));when(e.template){T.POLAROID->{Box(Modifier.fillMaxWidth().height(62.dp).align(Alignment.BottomCenter).background(Color.White)){Text("good things take time ♡",Modifier.align(Alignment.Center),fontSize=13.sp)}};T.BOW->Text("🎀",Modifier.align(Alignment.TopEnd).padding(12.dp),fontSize=35.sp);T.LAVENDER->Text("✦  ♡  ✦",Modifier.align(Alignment.BottomCenter).padding(12.dp),color=Color.White);T.GINGHAM->Text("♡",Modifier.align(Alignment.BottomEnd).padding(14.dp),color=PINK,fontSize=25.sp);T.FILM->Text("GIRLYCAM  •  01",Modifier.align(Alignment.BottomStart).padding(12.dp),color=Color.White,fontSize=10.sp);T.MINIMAL->Text("♡",Modifier.align(Alignment.BottomEnd).padding(12.dp),color=PINK,fontSize=22.sp)}}
fun bg(t:T)=when(t){T.POLAROID,T.MINIMAL->Color.White;T.FILM->Color(0xFF262228);T.LAVENDER->LAV;T.GINGHAM->Color(0xFFFFE7EE);T.BOW->Color(0xFFFFF0F4)}

@Composable fun Templates(back:()->Unit,pick:(T)->Unit){Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)){Row(verticalAlignment=Alignment.CenterVertically){IconButton(back){Icon(Icons.Rounded.ArrowBack,null)};Text("Templates",fontSize=25.sp,fontWeight=FontWeight.SemiBold)};Spacer(Modifier.height(14.dp));Text("Pick a mood",color=MUTED,fontSize=13.sp);Spacer(Modifier.height(14.dp));T.entries.chunked(2).forEach{r->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)){r.forEach{t->Column(Modifier.weight(1f).clip(RoundedCornerShape(21.dp)).background(Color.White).border(1.dp,BLUSH,RoundedCornerShape(21.dp)).clickable{pick(t)}.padding(10.dp)){Box(Modifier.fillMaxWidth().aspectRatio(.75f).clip(RoundedCornerShape(14.dp)).background(bg(t)),contentAlignment=Alignment.Center){Text(if(t==T.BOW)"🎀" else "✦  ♡  ✦",color=PINK,fontSize=20.sp)};Spacer(Modifier.height(7.dp));Text(t.label,fontWeight=FontWeight.Medium,fontSize=13.sp);Text("tap to use",color=MUTED,fontSize=10.sp)}}};Spacer(Modifier.height(12.dp))}}}
@Composable fun Collage(us:List<Uri>,back:()->Unit,save:()->Unit){Column(Modifier.fillMaxSize().padding(18.dp)){Row(verticalAlignment=Alignment.CenterVertically){IconButton(back){Icon(Icons.Rounded.ArrowBack,null)};Text("Collage",fontSize=25.sp,fontWeight=FontWeight.SemiBold,Modifier.weight(1f));Button(save,shape=RoundedCornerShape(15.dp),contentPadding=PaddingValues(horizontal=14.dp,vertical=8.dp)){Text("Save")}};Spacer(Modifier.height(15.dp));Box(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(25.dp)).background(LAV).padding(14.dp)){if(us.isEmpty())Text("Choose 2–4 photos to start",Modifier.align(Alignment.Center),color=MUTED) else Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(10.dp)){us.chunked(2).forEach{row->Row(Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(10.dp)){row.forEach{u->AsyncImage(u,null,Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(12.dp)))};if(row.size==1)Spacer(Modifier.weight(1f))}}}};Spacer(Modifier.height(12.dp));Text("soft lavender collage • 2–4 photos",fontSize=12.sp,color=MUTED)}}

fun bitmap(ctx:Context,u:Uri):Bitmap=if(Build.VERSION.SDK_INT>=28)ImageDecoder.decodeBitmap(ImageDecoder.createSource(ctx.contentResolver,u)) else @Suppress("DEPRECATION") MediaStore.Images.Media.getBitmap(ctx.contentResolver,u)
fun crop(b:Bitmap,w:Int,h:Int):Bitmap{val s=maxOf(w.toFloat()/b.width,h.toFloat()/b.height);val x=Bitmap.createScaledBitmap(b,(b.width*s).toInt(),(b.height*s).toInt(),true);return Bitmap.createBitmap(x,(x.width-w)/2,(x.height-h)/2,w,h)}
fun text(c:Canvas,s:String,x:Float,y:Float,size:Float,color:Int,center:Boolean=true){val p=Paint(1).apply{this.color=color;textSize=size;textAlign=if(center)Paint.Align.CENTER else Paint.Align.LEFT};c.drawText(s,x,y,p)}
fun render(ctx:Context,e:Edit):Bitmap{val w=1200;val h=(w/e.ratio).toInt();val out=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);val c=Canvas(out);val p=Paint(1);c.drawColor(bg(e.template).toArgb());val pad=if(e.template==T.POLAROID)70 else 45;val bottom=if(e.template==T.POLAROID)150 else pad;c.drawBitmap(crop(bitmap(ctx,e.uri),w-pad*2,h-pad-bottom),pad.toFloat(),pad.toFloat(),p);if(e.template==T.POLAROID){p.color=Color.WHITE.toArgb();c.drawRect(0f,(h-150).toFloat(),w.toFloat(),h.toFloat(),p);text(c,"good things take time ♡",w/2f,h-62f,34f,TXT.toArgb())};if(e.template==T.FILM){p.color=0x66FFFFFF;for(y in 35 until h step 70)c.drawRect(16f,y.toFloat(),24f,(y+35).toFloat(),p);text(c,"GIRLYCAM  •  01",48f,h-45f,26f,Color.WHITE.toArgb(),false)};if(e.template==T.LAVENDER)text(c,"✦  ♡  ✦",w/2f,h-35f,36f,Color.WHITE.toArgb());if(e.template==T.GINGHAM){p.color=0x22F58AA8;for(x in 0 until w step 55)c.drawRect(x.toFloat(),0f,(x+22).toFloat(),h.toFloat(),p);text(c,"♡",w-60f,h-45f,38f,PINK.toArgb())};if(e.template==T.BOW){text(c,"🎀",w-90f,85f,55f,Color.WHITE.toArgb());text(c,"♡",65f,h-45f,38f,PINK.toArgb())};return out}
fun save(ctx:Context,e:Edit){val b=render(ctx,e);val n="GirlyCam_${System.currentTimeMillis()}.jpg";val v=ContentValues().apply{put(MediaStore.Images.Media.DISPLAY_NAME,n);put(MediaStore.Images.Media.MIME_TYPE,"image/jpeg");if(Build.VERSION.SDK_INT>=29)put(MediaStore.Images.Media.RELATIVE_PATH,"Pictures/GirlyCam")};ctx.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,v)?.let{ctx.contentResolver.openOutputStream(it)?.use{b.compress(Bitmap.CompressFormat.JPEG,94,it)}}}
fun saveCollage(ctx:Context,us:List<Uri>){if(us.isEmpty())return;val w=1200;val b=Bitmap.createBitmap(w,w,Bitmap.Config.ARGB_8888);val c=Canvas(b);c.drawColor(LAV.toArgb());val p=Paint(1);val gap=22;val cell=(w-gap*3)/2;us.take(4).forEachIndexed{i,u->val x=gap+(i%2)*(cell+gap);val y=gap+(i/2)*(cell+gap);c.drawBitmap(crop(bitmap(ctx,u),cell,cell),x.toFloat(),y.toFloat(),p)};text(c,"made with ♡ GirlyCam",w/2f,w-48f,26f,TXT.toArgb());val n="GirlyCam_Collage_${System.currentTimeMillis()}.jpg";val v=ContentValues().apply{put(MediaStore.Images.Media.DISPLAY_NAME,n);put(MediaStore.Images.Media.MIME_TYPE,"image/jpeg");if(Build.VERSION.SDK_INT>=29)put(MediaStore.Images.Media.RELATIVE_PATH,"Pictures/GirlyCam")};ctx.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,v)?.let{ctx.contentResolver.openOutputStream(it)?.use{b.compress(Bitmap.CompressFormat.JPEG,94,it)}}}
