package tw.idempiere.form;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.compiere.model.MSysConfig;
import org.compiere.util.Env;

/**
 * 用 LibreOffice headless 把 Office 檔（Word/PPT 等）轉成 PDF，供附件預覽內嵌顯示。
 *
 * soffice 執行檔路徑由 SysConfig {@value #SYSCONFIG_PATH} 決定（預設 macOS 標準路徑），
 * 不同主機只要改 SysConfig 即可。每次轉檔用獨立暫存目錄與 UserInstallation，併發安全，轉完清暫存。
 *
 * （與 tw.topgiga.purchase 的同名工具內容相同，為讓兩個 plugin 互相獨立而各自保留一份。）
 */
public final class OfficeToPdf {

	private static final Logger log = Logger.getLogger(OfficeToPdf.class.getName());

	/** SysConfig：soffice 執行檔完整路徑。 */
	public static final String SYSCONFIG_PATH = "LIBREOFFICE_SOFFICE_PATH";
	/** SysConfig：LibreOffice 範本 profile 目錄（含字型替換表 registrymodifications.xcu）；未設則用全新空 profile。 */
	public static final String SYSCONFIG_PROFILE = "LIBREOFFICE_PROFILE_TEMPLATE";
	/** macOS 標準安裝路徑（未設 SysConfig 時的預設）。 */
	private static final String DEFAULT_PATH = "/Applications/LibreOffice.app/Contents/MacOS/soffice";
	/** 單次轉檔逾時（毫秒）。 */
	private static final long TIMEOUT_MS = 45000;

	private OfficeToPdf() {
	}

	/** 是否為需要轉檔才能預覽的 Office/文件格式。 */
	public static boolean isOffice(String name) {
		String n = (name == null) ? "" : name.toLowerCase();
		return n.endsWith(".doc") || n.endsWith(".docx") || n.endsWith(".rtf") || n.endsWith(".odt")
				|| n.endsWith(".ppt") || n.endsWith(".pptx") || n.endsWith(".odp")
				|| isSpreadsheet(name);
	}

	/** 是否為試算表（優先用 Keikai；轉檔時退回 HTML）。 */
	public static boolean isSpreadsheet(String name) {
		String n = (name == null) ? "" : name.toLowerCase();
		return n.endsWith(".xls") || n.endsWith(".xlsx") || n.endsWith(".ods") || n.endsWith(".csv");
	}

	/** 把 Office bytes 轉成 PDF（Word/PPT 等）；失敗回 null。 */
	public static byte[] toPdf(byte[] data, String filename) {
		return convert(data, filename, "pdf", "pdf");
	}

	/** 把試算表 bytes 轉成 HTML（整張表可捲，不分頁）；失敗回 null。 */
	public static byte[] toHtml(byte[] data, String filename) {
		return convert(data, filename, "html", "html");
	}

	private static byte[] convert(byte[] data, String filename, String convertTo, String outExt) {
		if (data == null || data.length == 0) {
			return null;
		}
		String soffice = MSysConfig.getValue(SYSCONFIG_PATH, DEFAULT_PATH, Env.getAD_Client_ID(Env.getCtx()));
		Path work = null;
		try {
			work = Files.createTempDirectory("po_lo_");
			String base = baseName(filename);
			File input = new File(work.toFile(), base + "." + ext(filename));
			Files.write(input.toPath(), data);
			File profile = new File(work.toFile(), "profile");
			seedProfile(profile); // 若有設定範本 profile（字型替換表），複製進來使用

			ProcessBuilder pb = new ProcessBuilder(
					soffice, "--headless", "--norestore", "--nolockcheck", "--nodefault",
					"--nofirststartwizard",
					"-env:UserInstallation=file://" + profile.getAbsolutePath(),
					"--convert-to", convertTo, "--outdir", work.toFile().getAbsolutePath(),
					input.getAbsolutePath());
			pb.redirectErrorStream(true);
			Process p = pb.start();
			byte[] out = readAll(p.getInputStream());
			boolean done = p.waitFor(TIMEOUT_MS, TimeUnit.MILLISECONDS);
			if (!done) {
				p.destroyForcibly();
				log.warning("soffice 轉檔逾時：" + filename);
				return null;
			}
			if (p.exitValue() != 0) {
				log.warning("soffice 轉檔失敗 exit=" + p.exitValue() + "，" + filename + "：" + new String(out));
				return null;
			}
			File result = new File(work.toFile(), base + "." + outExt);
			if (!result.exists()) {
				log.warning("soffice 未產生 " + outExt + "：" + filename);
				return null;
			}
			return Files.readAllBytes(result.toPath());
		} catch (Exception e) {
			log.log(Level.SEVERE, "convert 失敗：" + filename + "（soffice=" + soffice + "）", e);
			return null;
		} finally {
			if (work != null) {
				deleteQuietly(work.toFile());
			}
		}
	}

	/** 若 SysConfig 有設範本 profile 目錄，複製到本次轉檔的 profile（帶字型替換表）。 */
	private static void seedProfile(File dest) {
		String tpl = MSysConfig.getValue(SYSCONFIG_PROFILE, "", Env.getAD_Client_ID(Env.getCtx()));
		if (tpl == null || tpl.trim().length() == 0) {
			return;
		}
		File src = new File(tpl.trim());
		if (!src.isDirectory()) {
			log.warning("LIBREOFFICE_PROFILE_TEMPLATE 目錄不存在，改用空 profile：" + tpl);
			return;
		}
		try (Stream<Path> walk = Files.walk(src.toPath())) {
			Path from = src.toPath();
			Path to = dest.toPath();
			for (Path p : (Iterable<Path>) walk::iterator) {
				Path target = to.resolve(from.relativize(p).toString());
				if (Files.isDirectory(p)) {
					Files.createDirectories(target);
				} else {
					Files.createDirectories(target.getParent());
					Files.copy(p, target, StandardCopyOption.REPLACE_EXISTING);
				}
			}
		} catch (IOException e) {
			log.warning("複製 profile 範本失敗，改用空 profile：" + e.getMessage());
		}
	}

	private static String baseName(String name) {
		String n = (name == null) ? "file" : name;
		int slash = Math.max(n.lastIndexOf('/'), n.lastIndexOf('\\'));
		if (slash >= 0) {
			n = n.substring(slash + 1);
		}
		int dot = n.lastIndexOf('.');
		String b = (dot > 0) ? n.substring(0, dot) : n;
		b = b.replaceAll("[^\\w\\-]", "_");
		return b.length() == 0 ? "file" : b;
	}

	private static String ext(String name) {
		String n = (name == null) ? "" : name;
		int dot = n.lastIndexOf('.');
		String e = (dot >= 0) ? n.substring(dot + 1) : "";
		e = e.replaceAll("[^\\w]", "").toLowerCase();
		return e.length() == 0 ? "bin" : e;
	}

	private static byte[] readAll(InputStream in) {
		try {
			return in.readAllBytes();
		} catch (Exception e) {
			return new byte[0];
		}
	}

	private static void deleteQuietly(File f) {
		if (f == null) {
			return;
		}
		File[] kids = f.listFiles();
		if (kids != null) {
			for (File k : kids) {
				deleteQuietly(k);
			}
		}
		f.delete();
	}
}
