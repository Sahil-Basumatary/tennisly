import { fetchAdminHealthSnapshot } from "@/lib/admin-upstream";
import { noStoreJson } from "@/lib/admin-bff";

export async function GET() {
  const data = await fetchAdminHealthSnapshot();
  return noStoreJson(data);
}
